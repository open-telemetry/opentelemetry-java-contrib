package io.opentelemetry.opamp.client.internal.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import opamp.proto.AgentToServer;
import opamp.proto.RetryInfo;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class HttpRequestServiceTest {
  @Mock private HttpSender requestSender;
  @Mock private PeriodicDelay periodicRequestDelay;
  @Mock private TestPeriodicRetryDelay periodicRetryDelay;
  @Mock private PeriodicTaskExecutor executor;
  @Mock private RequestService.Callback callback;
  @Mock private Supplier<Request> requestSupplier;
  private int requestSize = -1;
  private HttpRequestService httpRequestService;

  @BeforeEach
  void setUp() {
    httpRequestService =
        new HttpRequestService(
            requestSender,
            executor,
            periodicRequestDelay,
            periodicRetryDelay,
            RetryAfterParser.getInstance());
  }

  @Test
  void verifyStart() {
    httpRequestService.start(callback, requestSupplier);

    InOrder inOrder = inOrder(periodicRequestDelay, executor);
    inOrder.verify(executor).start(httpRequestService);

    // Try starting it again:
    try {
      httpRequestService.start(callback, requestSupplier);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("RequestDispatcher is already running");
    }
  }

  @Test
  void verifyStop() {
    httpRequestService.start(callback, requestSupplier);
    httpRequestService.stop();

    verify(executor).stop();

    // Try stopping it again:
    clearInvocations(executor);
    httpRequestService.stop();
    verifyNoInteractions(executor);
  }

  @Test
  void verifyStop_whenNotStarted() {
    httpRequestService.stop();

    verifyNoInteractions(executor, requestSender, periodicRequestDelay);
  }

  @Test
  void whenTryingToStartAfterStopHasBeenCalled_throwException() {
    httpRequestService.start(callback, requestSupplier);
    httpRequestService.stop();
    try {
      httpRequestService.start(callback, requestSupplier);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("RequestDispatcher has been stopped");
    }
  }

  @Test
  void verifySendingRequest_happyPath() {
    HttpSender.Response httpResponse = mock();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().build();
    attachServerToAgentMessage(serverToAgent.encodeByteString().toByteArray(), httpResponse);
    prepareRequest();
    enqueueResponse(httpResponse);

    httpRequestService.run();

    verify(requestSender).send(any(), eq(requestSize));
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
    verify(callback).onRequestFailed(any());
  }

  @Test
  void verifySendingRequest_whenThereIsAnExecutionError()
      throws ExecutionException, InterruptedException {
    prepareRequest();
    CompletableFuture<HttpSender.Response> future = mock();
    when(requestSender.send(any(), anyInt())).thenReturn(future);
    Exception myException = mock();
    doThrow(new ExecutionException(myException)).when(future).get();

    httpRequestService.run();

    verify(requestSender).send(any(), eq(requestSize));
    verify(callback).onRequestFailed(myException);
  }

  @Test
  void verifySendingRequest_whenThereIsAnInterruptedException()
      throws ExecutionException, InterruptedException {
    prepareRequest();
    CompletableFuture<HttpSender.Response> future = mock();
    when(requestSender.send(any(), anyInt())).thenReturn(future);
    InterruptedException myException = mock();
    doThrow(myException).when(future).get();

    httpRequestService.run();

    verify(requestSender).send(any(), eq(requestSize));
    verify(callback).onRequestFailed(myException);
  }

  @Test
  void verifySendingRequest_whenThereIsAGenericHttpError() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(500);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestFailed(new HttpErrorException(500, "Error message"));
    verifyNoInteractions(executor);
  }

  @Test
  void verifySendingRequest_whenThereIsATooManyRequestsError() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(429);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestFailed(new HttpErrorException(429, "Error message"));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
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

    verify(callback).onRequestFailed(new HttpErrorException(429, "Error message"));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
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
    verify(executor).setPeriodicDelay(periodicRetryDelay);
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
    verify(executor).setPeriodicDelay(periodicRetryDelay);
  }

  @Test
  void verifySendingRequest_whenThereIsAServiceUnavailableError() {
    HttpSender.Response response = mock();
    when(response.statusCode()).thenReturn(503);
    when(response.statusMessage()).thenReturn("Error message");
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestFailed(new HttpErrorException(503, "Error message"));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
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

    verify(callback).onRequestFailed(new HttpErrorException(503, "Error message"));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
    verify(periodicRetryDelay).suggestDelay(Duration.ofSeconds(2));
  }

  @Test
  void verifySendingRequest_duringRegularMode() {
    httpRequestService.sendRequest();

    verify(executor).executeNow();
  }

  @Test
  void verifySendingRequest_duringRetryMode() {
    enableRetryMode();

    httpRequestService.sendRequest();

    verify(executor, never()).executeNow();
  }

  @Test
  void verifySuccessfulSendingRequest_duringRetryMode() {
    enableRetryMode();
    HttpSender.Response response = mock();
    attachServerToAgentMessage(
        new ServerToAgent.Builder().build().encodeByteString().toByteArray(), response);
    enqueueResponse(response);

    httpRequestService.run();

    verify(executor).setPeriodicDelay(periodicRequestDelay);
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
    httpRequestService.start(callback, requestSupplier);
    clearInvocations(executor);
    AgentToServer agentToServer = new AgentToServer.Builder().sequence_num(10).build();
    requestSize = agentToServer.encodeByteString().size();
    Request request = Request.create(agentToServer);
    when(requestSupplier.get()).thenReturn(request);
  }

  private void enqueueResponse(HttpSender.Response httpResponse) {
    when(requestSender.send(any(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(httpResponse));
  }

  private static void attachServerToAgentMessage(
      byte[] serverToAgent, HttpSender.Response httpResponse) {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serverToAgent);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyInputStream()).thenReturn(byteArrayInputStream);
  }

  private static class TestPeriodicRetryDelay implements PeriodicDelay, AcceptsDelaySuggestion {

    @Override
    public void suggestDelay(Duration delay) {}

    @Override
    public Duration getNextDelay() {
      return null;
    }

    @Override
    public void reset() {}
  }
}
