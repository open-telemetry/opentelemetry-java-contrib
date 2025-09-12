/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.CodedOutputStream;
import io.opentelemetry.opamp.client.internal.connectivity.websocket.WebSocket;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseException;
import io.opentelemetry.opamp.client.internal.response.Response;
import io.opentelemetry.opamp.client.request.service.RequestService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import opamp.proto.AgentToServer;
import opamp.proto.RetryInfo;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebSocketRequestServiceTest {
  @Mock private WebSocket webSocket;
  @Mock private RequestService.Callback callback;
  @Mock private PeriodicDelayWithSuggestion retryDelay;
  private Request request;
  private TestScheduler scheduler;
  private WebSocketRequestService requestService;
  private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);

  @BeforeEach
  void setUp() {
    lenient().when(retryDelay.getNextDelay()).thenReturn(INITIAL_RETRY_DELAY);
    scheduler = new TestScheduler();
    requestService = new WebSocketRequestService(webSocket, retryDelay, scheduler.getMockService());
  }

  @Test
  void verifySuccessfulStart() {
    startService();
    verify(webSocket).open(requestService);

    // When opening successfully, notify callback
    requestService.onOpen();
    verify(callback).onConnectionSuccess();
    verifyNoMoreInteractions(callback);

    // It shouldn't allow starting again
    try {
      startService();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The service has already started");
    }
  }

  @Test
  void verifyFailedStart() {
    startService();
    verify(webSocket).open(requestService);

    // When failing while opening, notify callback
    Throwable t = mock();
    requestService.onFailure(t);
    verify(retryDelay).reset();
    verify(callback).onConnectionFailed(t);
    verifyNoMoreInteractions(callback);

    // Check connection retry is scheduled
    assertThat(scheduler.getScheduledTasks()).hasSize(1);
    assertThat(scheduler.getScheduledTasks().get(0).getDelay()).isEqualTo(INITIAL_RETRY_DELAY);

    // It shouldn't allow starting again
    try {
      startService();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The service has already started");
    }

    // It shouldn't schedule more than one retry at a time
    clearInvocations(retryDelay, callback);
    requestService.onFailure(t);
    verify(callback).onConnectionFailed(t);
    verifyNoInteractions(retryDelay);
    verifyNoMoreInteractions(callback);
    assertThat(scheduler.getScheduledTasks()).hasSize(1);

    // Execute retry with new delay
    clearInvocations(webSocket, callback);
    when(retryDelay.getNextDelay()).thenReturn(Duration.ofSeconds(5));
    scheduler.getScheduledTasks().get(0).run();
    assertThat(scheduler.getScheduledTasks()).isEmpty();
    verify(webSocket).open(requestService);

    // Fail again
    requestService.onFailure(t);
    verify(retryDelay, never()).reset();
    verify(callback).onConnectionFailed(t);

    // A new retry has been scheduled
    assertThat(scheduler.getScheduledTasks()).hasSize(1);
    assertThat(scheduler.getScheduledTasks().get(0).getDelay()).isEqualTo(Duration.ofSeconds(5));

    // Execute retry again
    clearInvocations(webSocket, callback);
    scheduler.getScheduledTasks().get(0).run();
    assertThat(scheduler.getScheduledTasks()).isEmpty();
    verify(webSocket).open(requestService);

    // Succeed
    requestService.onOpen();
    verify(callback).onConnectionSuccess();
    verifyNoMoreInteractions(callback);

    // Fail at some point
    clearInvocations(callback);
    requestService.onFailure(t);
    verify(callback).onConnectionFailed(t);
    verifyNoMoreInteractions(callback);
    verify(retryDelay).reset();
    assertThat(scheduler.getScheduledTasks()).hasSize(1);
  }

  @Test
  void verifySendRequest() {
    // Validate when not running
    try {
      requestService.sendRequest();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The service is not running");
    }

    startService();

    // Successful send
    when(webSocket.send(any())).thenReturn(true);
    requestService.sendRequest();
    verify(webSocket).send(getExpectedOutgoingBytes());

    // Check there are no pending requests
    clearInvocations(webSocket);
    requestService.onOpen();
    verifyNoInteractions(webSocket);

    // Failed send
    when(webSocket.send(any())).thenReturn(false);
    requestService.sendRequest();
    clearInvocations(webSocket);

    // Check pending request
    when(webSocket.send(any())).thenReturn(true);
    requestService.onOpen();
    verify(webSocket).send(getExpectedOutgoingBytes());
  }

  @Test
  void verifyOnMessage() {
    startService();

    // Successful message
    ServerToAgent serverToAgent = new ServerToAgent.Builder().build();
    requestService.onMessage(createServerToAgentPayload(serverToAgent));
    verify(callback).onRequestSuccess(Response.create(serverToAgent));
    verifyNoMoreInteractions(callback);
    assertThat(scheduler.getScheduledTasks()).isEmpty();

    // Regular error message
    ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
    clearInvocations(callback);
    serverToAgent =
        new ServerToAgent.Builder()
            .error_response(new ServerErrorResponse.Builder().error_message("A message").build())
            .build();
    requestService.onMessage(createServerToAgentPayload(serverToAgent));
    verify(callback).onRequestFailed(throwableCaptor.capture());
    verifyNoMoreInteractions(callback);
    OpampServerResponseException error = (OpampServerResponseException) throwableCaptor.getValue();
    assertThat(error.getMessage()).isEqualTo("A message");
    assertThat(scheduler.getScheduledTasks()).isEmpty();

    // Error message with unavailable status
    clearInvocations(callback);
    serverToAgent =
        new ServerToAgent.Builder()
            .error_response(
                new ServerErrorResponse.Builder()
                    .type(ServerErrorResponseType.ServerErrorResponseType_Unavailable)
                    .error_message("Try later")
                    .build())
            .build();
    requestService.onMessage(createServerToAgentPayload(serverToAgent));
    verify(callback).onRequestFailed(throwableCaptor.capture());
    verifyNoMoreInteractions(callback);
    OpampServerResponseException unavailableError =
        (OpampServerResponseException) throwableCaptor.getValue();
    assertThat(unavailableError.getMessage()).isEqualTo("Try later");
    assertThat(scheduler.getScheduledTasks()).hasSize(1);
    verify(retryDelay, never()).suggestDelay(any());

    // Reset scheduled retry
    scheduler.getScheduledTasks().get(0).run();
    requestService.onOpen();

    // Error message with unavailable status and suggested delay
    Duration suggestedDelay = Duration.ofSeconds(10);
    clearInvocations(callback, retryDelay);
    serverToAgent =
        new ServerToAgent.Builder()
            .error_response(
                new ServerErrorResponse.Builder()
                    .type(ServerErrorResponseType.ServerErrorResponseType_Unavailable)
                    .retry_info(
                        new RetryInfo.Builder()
                            .retry_after_nanoseconds(suggestedDelay.toNanos())
                            .build())
                    .build())
            .build();
    requestService.onMessage(createServerToAgentPayload(serverToAgent));
    verify(callback).onRequestFailed(throwableCaptor.capture());
    verifyNoMoreInteractions(callback);
    OpampServerResponseException unavailableErrorWithSuggestedDelay =
        (OpampServerResponseException) throwableCaptor.getValue();
    assertThat(unavailableErrorWithSuggestedDelay.getMessage()).isEmpty();
    assertThat(scheduler.getScheduledTasks()).hasSize(1);
    verify(retryDelay).suggestDelay(suggestedDelay);
  }

  @Test
  void verifyStop() {
    startService();

    requestService.stop();

    InOrder inOrder = inOrder(webSocket);
    inOrder.verify(webSocket).send(getExpectedOutgoingBytes());
    inOrder.verify(webSocket).close(1000, null);
    verify(scheduler.getMockService()).shutdown();

    // If something fails afterward, no retry should get scheduled.
    requestService.onFailure(mock());
    verifyNoInteractions(retryDelay);
    assertThat(scheduler.getScheduledTasks()).isEmpty();

    // If onClosed is called afterward, no retry should get scheduled.
    requestService.onClosed();
    verifyNoInteractions(retryDelay);
    assertThat(scheduler.getScheduledTasks()).isEmpty();

    // If a new message with a server unavailable error arrives afterward, no retry should get
    // scheduled.
    ServerToAgent serverToAgent =
        new ServerToAgent.Builder()
            .error_response(
                new ServerErrorResponse.Builder()
                    .type(ServerErrorResponseType.ServerErrorResponseType_Unavailable)
                    .build())
            .build();
    requestService.onMessage(createServerToAgentPayload(serverToAgent));
    verifyNoInteractions(retryDelay);
    assertThat(scheduler.getScheduledTasks()).isEmpty();

    // Requests cannot get enqueued afterward.
    try {
      requestService.sendRequest();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("This service is already stopped");
    }

    // The service cannot get restarted afterward.
    try {
      startService();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("This service is already stopped");
    }
  }

  private byte[] getExpectedOutgoingBytes() {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      CodedOutputStream codedOutput = CodedOutputStream.newInstance(outputStream);
      codedOutput.writeUInt64NoTag(0);
      byte[] payload = request.getAgentToServer().encode();
      codedOutput.writeRawBytes(payload);
      codedOutput.flush();
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] createServerToAgentPayload(ServerToAgent serverToAgent) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      CodedOutputStream codedOutput = CodedOutputStream.newInstance(outputStream);
      codedOutput.writeUInt64NoTag(0);
      codedOutput.writeRawBytes(serverToAgent.encode());
      codedOutput.flush();
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void startService() {
    requestService.start(callback, this::createRequest);
  }

  private Request createRequest() {
    AgentToServer agentToServer = new AgentToServer.Builder().sequence_num(10).build();
    request = Request.create(agentToServer);
    return request;
  }
}
