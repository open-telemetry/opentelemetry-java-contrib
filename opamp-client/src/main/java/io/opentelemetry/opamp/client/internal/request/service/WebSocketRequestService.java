/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.service;

import com.squareup.wire.ProtoAdapter;
import io.opentelemetry.opamp.client.internal.connectivity.websocket.WebSocket;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.delay.AcceptsDelaySuggestion;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.delay.RetryPeriodicDelay;
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseException;
import io.opentelemetry.opamp.client.internal.response.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;

public final class WebSocketRequestService implements RequestService, WebSocket.Listener {
  private static final PeriodicDelay DEFAULT_DELAY_BETWEEN_RETRIES =
      RetryPeriodicDelay.create(Duration.ofSeconds(30));

  private final WebSocket webSocket;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final AtomicBoolean hasStopped = new AtomicBoolean(false);
  private final ConnectionStatus connectionStatus;
  private final ScheduledExecutorService executorService;

  /** Defined <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">here</a>. */
  private static final int WEBSOCKET_NORMAL_CLOSURE_CODE = 1000;

  @GuardedBy("hasPendingRequestLock")
  private boolean hasPendingRequest = false;

  private final Object hasPendingRequestLock = new Object();
  @Nullable private Callback callback;
  @Nullable private Supplier<Request> requestSupplier;

  /**
   * Creates an {@link WebSocketRequestService}.
   *
   * @param webSocket The WebSocket implementation.
   */
  public static WebSocketRequestService create(WebSocket webSocket) {
    return create(webSocket, DEFAULT_DELAY_BETWEEN_RETRIES);
  }

  /**
   * Creates an {@link WebSocketRequestService}.
   *
   * @param webSocket The WebSocket implementation.
   * @param periodicRetryDelay The time to wait between retries.
   */
  public static WebSocketRequestService create(
      WebSocket webSocket, PeriodicDelay periodicRetryDelay) {
    return new WebSocketRequestService(
        webSocket,
        periodicRetryDelay,
        Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory()));
  }

  WebSocketRequestService(
      WebSocket webSocket,
      PeriodicDelay periodicRetryDelay,
      ScheduledExecutorService executorService) {
    this.webSocket = webSocket;
    this.executorService = executorService;
    this.connectionStatus = new ConnectionStatus(periodicRetryDelay);
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    if (hasStopped.get()) {
      throw new IllegalStateException("This service is already stopped");
    }
    if (isRunning.compareAndSet(false, true)) {
      this.callback = callback;
      this.requestSupplier = requestSupplier;
      startConnection();
    } else {
      throw new IllegalStateException("The service has already started");
    }
  }

  private void startConnection() {
    webSocket.open(this);
  }

  @Override
  public void sendRequest() {
    if (!isRunning.get()) {
      throw new IllegalStateException("The service is not running");
    }
    if (hasStopped.get()) {
      throw new IllegalStateException("This service is already stopped");
    }

    doSendRequest();
  }

  private void doSendRequest() {
    try {
      synchronized (hasPendingRequestLock) {
        if (!trySendRequest()) {
          hasPendingRequest = true;
        }
      }
    } catch (IOException e) {
      getCallback().onRequestFailed(e);
    }
  }

  private boolean trySendRequest() throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ProtoAdapter.UINT64.encode(outputStream, 0L);
      byte[] payload = getRequest().getAgentToServer().encode();
      outputStream.write(payload);
      return webSocket.send(outputStream.toByteArray());
    }
  }

  @Nonnull
  private Request getRequest() {
    return Objects.requireNonNull(requestSupplier).get();
  }

  @Override
  public void stop() {
    if (hasStopped.compareAndSet(false, true)) {
      /*
       Sending last message as explained in the spec:
       https://opentelemetry.io/docs/specs/opamp/#websocket-transport-opamp-client-initiated.
       The client implementation must ensure that the "agent_disconnect" field will be provided in the
       next supplied request body.
      */
      doSendRequest();
      webSocket.close(WEBSOCKET_NORMAL_CLOSURE_CODE, null);
      executorService.shutdown();
    }
  }

  @Override
  public void onOpen() {
    connectionStatus.success();
    getCallback().onConnectionSuccess();
    synchronized (hasPendingRequestLock) {
      if (hasPendingRequest) {
        hasPendingRequest = false;
        sendRequest();
      }
    }
  }

  @Override
  public void onMessage(byte[] data) {
    try {
      ServerToAgent serverToAgent = readServerToAgent(data);

      ServerErrorResponse errorResponse = serverToAgent.error_response;
      if (errorResponse != null) {
        handleServerError(errorResponse);
        getCallback()
            .onRequestFailed(
                new OpampServerResponseException(errorResponse, errorResponse.error_message));
        return;
      }

      getCallback().onRequestSuccess(Response.create(serverToAgent));
    } catch (IOException e) {
      getCallback().onRequestFailed(e);
    }
  }

  private static ServerToAgent readServerToAgent(byte[] data) throws IOException {
    int headerSize = ProtoAdapter.UINT64.encodedSize(ProtoAdapter.UINT64.decode(data));
    int payloadSize = data.length - headerSize;
    byte[] payload = new byte[payloadSize];
    System.arraycopy(data, headerSize, payload, 0, payloadSize);
    return ServerToAgent.ADAPTER.decode(payload);
  }

  private void handleServerError(ServerErrorResponse errorResponse) {
    if (serverIsUnavailable(errorResponse)) {
      Duration retryAfter = null;

      if (errorResponse.retry_info != null) {
        retryAfter = Duration.ofNanos(errorResponse.retry_info.retry_after_nanoseconds);
      }

      webSocket.close(WEBSOCKET_NORMAL_CLOSURE_CODE, null);
      connectionStatus.retryAfter(retryAfter);
    }
  }

  private static boolean serverIsUnavailable(ServerErrorResponse errorResponse) {
    return errorResponse.type.equals(ServerErrorResponseType.ServerErrorResponseType_Unavailable);
  }

  @Override
  public void onClosing() {
    // Noop
  }

  @Override
  public void onClosed() {
    // If this service isn't stopped, we should retry connecting.
    connectionStatus.retryAfter(null);
  }

  @Override
  public void onFailure(Throwable t) {
    getCallback().onConnectionFailed(t);
    connectionStatus.retryAfter(null);
  }

  @Nonnull
  private Callback getCallback() {
    return Objects.requireNonNull(callback);
  }

  private class ConnectionStatus {
    private final PeriodicDelay periodicRetryDelay;
    private final AtomicBoolean retryingConnection = new AtomicBoolean(false);
    private final AtomicBoolean nextRetryScheduled = new AtomicBoolean(false);

    ConnectionStatus(PeriodicDelay periodicRetryDelay) {
      this.periodicRetryDelay = periodicRetryDelay;
    }

    void success() {
      retryingConnection.set(false);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void retryAfter(@Nullable Duration retryAfter) {
      if (hasStopped.get()) {
        return;
      }

      if (retryingConnection.compareAndSet(false, true)) {
        periodicRetryDelay.reset();
        if (retryAfter != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
          ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(retryAfter);
        }
      }

      if (nextRetryScheduled.compareAndSet(false, true)) {
        executorService.schedule(
            this::retryConnection,
            periodicRetryDelay.getNextDelay().toNanos(),
            TimeUnit.NANOSECONDS);
      }
    }

    private void retryConnection() {
      nextRetryScheduled.set(false);
      startConnection();
    }
  }
}
