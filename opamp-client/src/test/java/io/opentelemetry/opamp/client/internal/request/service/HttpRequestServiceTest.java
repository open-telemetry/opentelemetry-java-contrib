/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.opamp.client.internal.connectivity.http.HttpErrorException;
import io.opentelemetry.opamp.client.internal.connectivity.http.HttpSender;
import io.opentelemetry.opamp.client.internal.connectivity.http.RetryAfterParser;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.delay.AcceptsDelaySuggestion;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicTaskExecutor;
import io.opentelemetry.opamp.client.internal.response.Response;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import opamp.proto.AgentToServer;
import opamp.proto.RetryInfo;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class HttpRequestServiceTest {
  @Mock private RequestService.Callback callback;
  @Mock private Supplier<Request> requestSupplier;
  private TestHttpSender requestSender;
  private final PeriodicDelay periodicRequestDelay =
      PeriodicDelay.ofFixedDuration(Duration.ofSeconds(1));
  private TestPeriodicRetryDelay periodicRetryDelay;
  private int requestSize = -1;
  private HttpRequestService httpRequestService;

  @BeforeEach
  void setUp() {
    requestSender = new TestHttpSender();
    periodicRetryDelay = new TestPeriodicRetryDelay(Duration.ofSeconds(2));
    httpRequestService =
        new HttpRequestService(
            requestSender,
            PeriodicTaskExecutor.create(periodicRequestDelay),
            periodicRequestDelay,
            periodicRetryDelay,
            RetryAfterParser.getInstance());
    httpRequestService.start(callback, requestSupplier);
  }

  @AfterEach
  void tearDown() {
    requestSender.close();
    httpRequestService.stop();
  }

  @Test
  void whenTryingToStartAfterStopHasBeenCalled_throwException() {
    httpRequestService.start(callback, requestSupplier);
    httpRequestService.stop();
    try {
      httpRequestService.start(callback, requestSupplier);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("HttpRequestService cannot start after it has been stopped.");
    }
  }

  @Test
  void verifySendingRequest_happyPath() {
    HttpSender.Response httpResponse = mock();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().build();
    attachServerToAgentMessage(serverToAgent.encodeByteString().toByteArray(), httpResponse);
    prepareRequest();
    enqueueResponse(httpResponse);

    httpRequestService.sendRequest();

    requestSender.awaitForRequest(Duration.ofMillis(500));
    assertThat(requestSender.requests).hasSize(1);
    assertThat(requestSender.requests.get(0).contentLength).isEqualTo(requestSize);
    verify(callback).onConnectionSuccess();
    verify(callback).onRequestSuccess(Response.create(serverToAgent));
  }

  @Test
  void verifySendingRequest_whenTheresAParsingError() {
    HttpSender.Response httpResponse = mock();
    attachServerToAgentMessage(new byte[] {1, 2, 3}, httpResponse);
    prepareRequest();
    enqueueResponse(httpResponse);

    httpRequestService.run();

    verify(requestSender).send(any(), eq(requestSize));
    verify(callback).onConnectionFailed(any());
  }

  @Test
  void verifySendingRequest_whenThereIsAnExecutionError()
      throws ExecutionException, InterruptedException, TimeoutException {
    prepareRequest();
    CompletableFuture<HttpSender.Response> future = mock();
    when(requestSender.send(any(), anyInt())).thenReturn(future);
    Exception myException = mock();
    doThrow(new ExecutionException(myException)).when(future).get(30, TimeUnit.SECONDS);

    httpRequestService.run();

    verify(requestSender).send(any(), eq(requestSize));
    verify(callback).onConnectionFailed(myException);
  }

  @Test
  void verifySendingRequest_whenThereIsAnInterruptedException()
      throws ExecutionException, InterruptedException, TimeoutException {
    prepareRequest();
    CompletableFuture<HttpSender.Response> future = mock();
    when(requestSender.send(any(), anyInt())).thenReturn(future);
    InterruptedException myException = mock();
    doThrow(myException).when(future).get(30, TimeUnit.SECONDS);

    httpRequestService.run();

    verify(requestSender).send(any(), eq(requestSize));
    verify(callback).onConnectionFailed(myException);
  }

  @Test
  void verifySendingRequest_whenThereIsAGenericHttpError() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(500);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verifyRequestFailedCallback(500);
  }

  @Test
  void verifySendingRequest_whenThereIsATooManyRequestsError() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(429);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verifyRequestFailedCallback(429);
    //    verify(executor).setPeriodicDelay(periodicRetryDelay); todo
    verify(periodicRetryDelay, never()).suggestDelay(any());
  }

  @Test
  void verifySendingRequest_whenThereIsATooManyRequestsError_withSuggestedDelay() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(429);
    when(response.statusMessage()).thenReturn("Error message");
    when(response.getHeader("Retry-After")).thenReturn("5");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verifyRequestFailedCallback(429);
    //    verify(executor).setPeriodicDelay(periodicRetryDelay); todo
    verify(periodicRetryDelay).suggestDelay(Duration.ofSeconds(5));
  }

  @Test
  void verifySendingRequest_whenServerProvidesRetryInfo_usingTheProvidedInfo() {
    HttpSender.Response response = mock();
    long nanosecondsToWaitForRetry = 1000;
    ServerErrorResponse errorResponse =
        new ServerErrorResponse.Builder()
            .type(ServerErrorResponseType.ServerErrorResponseType_Unavailable)
            .retry_info(
                new RetryInfo.Builder().retry_after_nanoseconds(nanosecondsToWaitForRetry).build())
            .build();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().error_response(errorResponse).build();
    attachServerToAgentMessage(serverToAgent.encodeByteString().toByteArray(), response);
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestSuccess(Response.create(serverToAgent));
    verify(periodicRetryDelay).suggestDelay(Duration.ofNanos(nanosecondsToWaitForRetry));
    //    verify(executor).setPeriodicDelay(periodicRetryDelay); todo
  }

  @Test
  void verifySendingRequest_whenServerIsUnavailable() {
    HttpSender.Response response = mock();
    ServerErrorResponse errorResponse =
        new ServerErrorResponse.Builder()
            .type(ServerErrorResponseType.ServerErrorResponseType_Unavailable)
            .build();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().error_response(errorResponse).build();
    attachServerToAgentMessage(serverToAgent.encodeByteString().toByteArray(), response);
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestSuccess(Response.create(serverToAgent));
    verify(periodicRetryDelay, never()).suggestDelay(any());
    //    verify(executor).setPeriodicDelay(periodicRetryDelay); todo
  }

  @Test
  void verifySendingRequest_whenThereIsAServiceUnavailableError() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(503);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verifyRequestFailedCallback(503);
    //    verify(executor).setPeriodicDelay(periodicRetryDelay); todo
    verify(periodicRetryDelay, never()).suggestDelay(any());
  }

  @Test
  void verifySendingRequest_whenThereIsAServiceUnavailableError_withSuggestedDelay() {
    HttpSender.Response response = mock();
    when(response.getHeader("Retry-After")).thenReturn("2");
    when(response.statusCode()).thenReturn(503);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verifyRequestFailedCallback(503);
    //    verify(executor).setPeriodicDelay(periodicRetryDelay); todo
    verify(periodicRetryDelay).suggestDelay(Duration.ofSeconds(2));
  }

  private void verifyRequestFailedCallback(int errorCode) {
    ArgumentCaptor<HttpErrorException> captor = ArgumentCaptor.forClass(HttpErrorException.class);
    verify(callback).onRequestFailed(captor.capture());
    assertThat(captor.getValue().getErrorCode()).isEqualTo(errorCode);
    assertThat(captor.getValue().getMessage()).isEqualTo("Error message");
  }

  @Test
  void verifySendingRequest_duringRegularMode() {
    httpRequestService.sendRequest();

    //    verify(executor).executeNow(); todo
  }

  @Test
  void verifySendingRequest_duringRetryMode() {
    enableRetryMode();

    httpRequestService.sendRequest();

    //    verify(executor, never()).executeNow(); todo
  }

  @Test
  void verifySuccessfulSendingRequest_duringRetryMode() {
    enableRetryMode();
    HttpSender.Response response = mock();
    attachServerToAgentMessage(
        new ServerToAgent.Builder().build().encodeByteString().toByteArray(), response);
    enqueueResponse(response);

    httpRequestService.run();

    //    verify(executor).setPeriodicDelay(periodicRequestDelay); todo
  }

  private void enableRetryMode() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(503);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();
  }

  private void prepareRequest() {
    AgentToServer agentToServer = new AgentToServer.Builder().sequence_num(10).build();
    requestSize = agentToServer.encodeByteString().size();
    Request request = Request.create(agentToServer);
    when(requestSupplier.get()).thenReturn(request);
  }

  private void enqueueResponse(HttpSender.Response httpResponse) {
    requestSender.enqueueResponse(httpResponse);
  }

  private static void attachServerToAgentMessage(
      byte[] serverToAgent, HttpSender.Response httpResponse) {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serverToAgent);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyInputStream()).thenReturn(byteArrayInputStream);
  }

  private static class TestPeriodicRetryDelay implements PeriodicDelay, AcceptsDelaySuggestion {
    private final Duration delay;

    private TestPeriodicRetryDelay(Duration delay) {
      this.delay = delay;
    }

    @Override
    public void suggestDelay(Duration delay) {}

    @Override
    public Duration getNextDelay() {
      return delay;
    }

    @Override
    public void reset() {}
  }

  private static class TestHttpSender implements HttpSender, Closeable {
    private final List<RequestParams> requests = Collections.synchronizedList(new ArrayList<>());
    private final Queue<HttpSender.Response> responses = new ConcurrentLinkedQueue<>();
    private final AtomicInteger unexpectedRequests = new AtomicInteger(0);
    private volatile CountDownLatch latch;

    @Override
    public CompletableFuture<HttpSender.Response> send(BodyWriter writer, int contentLength) {
      requests.add(new RequestParams(contentLength));
      HttpSender.Response response = null;
      try {
        response = responses.remove();
        if (latch != null) {
          latch.countDown();
        }
      } catch (NoSuchElementException e) {
        unexpectedRequests.incrementAndGet();
      }
      return CompletableFuture.completedFuture(response);
    }

    public void enqueueResponse(HttpSender.Response response) {
      responses.add(response);
    }

    public void awaitForRequest(Duration timeout) {
      if (latch != null) {
        throw new IllegalStateException();
      }
      latch = new CountDownLatch(1);
      try {
        if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
          fail("No request received before timeout " + timeout);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        latch = null;
      }
    }

    @Override
    public void close() {
      int count = unexpectedRequests.get();
      if (count > 0) {
        fail("Unexpected requests count: " + count);
      }
    }

    private static class RequestParams {
      public final int contentLength;

      private RequestParams(int contentLength) {
        this.contentLength = contentLength;
      }
    }
  }
}
