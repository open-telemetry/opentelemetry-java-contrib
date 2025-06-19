package io.opentelemetry.opamp.client.internal.request.service;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import io.opentelemetry.opamp.client.internal.connectivity.websocket.WebSocket;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.delay.AcceptsDelaySuggestion;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseError;
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
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;

public final class WebSocketRequestService implements RequestService, WebSocket.Listener {
  private final WebSocket webSocket;
  private final PeriodicDelay periodicRetryDelay;
  private final AtomicBoolean retryingConnection = new AtomicBoolean(false);
  private final AtomicBoolean nextRetryScheduled = new AtomicBoolean(false);
  private final AtomicBoolean hasPendingRequest = new AtomicBoolean(false);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ScheduledExecutorService executorService;
  public static final PeriodicDelay DEFAULT_DELAY_BETWEEN_RETRIES =
      PeriodicDelay.ofFixedDuration(Duration.ofSeconds(30));
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
        webSocket, periodicRetryDelay, Executors.newSingleThreadScheduledExecutor());
  }

  WebSocketRequestService(
      WebSocket webSocket,
      PeriodicDelay periodicRetryDelay,
      ScheduledExecutorService executorService) {
    this.webSocket = webSocket;
    this.periodicRetryDelay = periodicRetryDelay;
    this.executorService = executorService;
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    if (closed.get()) {
      throw new IllegalStateException("This service is already closed");
    }
    this.callback = callback;
    this.requestSupplier = requestSupplier;
    startConnection();
  }

  private void startConnection() {
    webSocket.open(this);
  }

  @Override
  public void sendRequest() {
    try {
      if (!trySendRequest()) {
        hasPendingRequest.set(true);
      }
    } catch (IOException e) {
      getCallback().onRequestFailed(e);
    }
  }

  private boolean trySendRequest() throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      CodedOutputStream codedOutput = CodedOutputStream.newInstance(outputStream);
      codedOutput.writeUInt64NoTag(0);
      byte[] payload = getRequest().getAgentToServer().encode();
      codedOutput.write(payload, 0, payload.length);
      codedOutput.flush();
      return webSocket.send(outputStream.toByteArray());
    }
  }

  @Nonnull
  private Request getRequest() {
    return Objects.requireNonNull(requestSupplier).get();
  }

  @Override
  public void stop() {
    if (closed.compareAndSet(false, true)) {
      sendRequest();
      webSocket.close(1000, null);
    }
  }

  @Override
  public void onOpen() {
    retryingConnection.set(false);
    getCallback().onConnectionSuccess();
    if (hasPendingRequest.compareAndSet(true, false)) {
      sendRequest();
    }
  }

  @Override
  public void onMessage(byte[] data) {
    try {
      ServerToAgent serverToAgent = readServerToAgent(data);

      if (serverToAgent.error_response != null) {
        handleServerError(serverToAgent.error_response);
        getCallback()
            .onRequestFailed(
                new OpampServerResponseError(serverToAgent.error_response.error_message));
        return;
      }

      getCallback().onRequestSuccess(Response.create(serverToAgent));
    } catch (IOException e) {
      getCallback().onRequestFailed(e);
    }
  }

  private static ServerToAgent readServerToAgent(byte[] data) throws IOException {
    CodedInputStream codedInputStream = CodedInputStream.newInstance(data);
    codedInputStream.readRawVarint64(); // It moves the read position to the end of the header.
    int totalBytesRead = codedInputStream.getTotalBytesRead();
    int payloadSize = data.length - totalBytesRead;
    byte[] payload = new byte[payloadSize];
    System.arraycopy(data, totalBytesRead, payload, 0, payloadSize);
    return ServerToAgent.ADAPTER.decode(payload);
  }

  private void handleServerError(ServerErrorResponse errorResponse) {
    if (serverIsUnavailable(errorResponse)) {
      Duration retryAfter = null;

      if (errorResponse.retry_info != null) {
        retryAfter = Duration.ofNanos(errorResponse.retry_info.retry_after_nanoseconds);
      }

      webSocket.close(1000, null);
      scheduleConnectionRetry(retryAfter);
    }
  }

  private static boolean serverIsUnavailable(ServerErrorResponse errorResponse) {
    return errorResponse.type.equals(ServerErrorResponseType.ServerErrorResponseType_Unavailable);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void scheduleConnectionRetry(@Nullable Duration retryAfter) {
    if (retryingConnection.compareAndSet(false, true)) {
      periodicRetryDelay.reset();
      if (retryAfter != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
        ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(retryAfter);
      }
    }
    if (nextRetryScheduled.compareAndSet(false, true)) {
      executorService.schedule(
          this::retryConnection, periodicRetryDelay.getNextDelay().toNanos(), TimeUnit.NANOSECONDS);
    }
  }

  private void retryConnection() {
    nextRetryScheduled.set(false);
    startConnection();
  }

  @Override
  public void onClosed() {
    if (!closed.get()) {
      // The service isn't closed so we should retry connecting.
      scheduleConnectionRetry(null);
    }
  }

  @Override
  public void onFailure(Throwable t) {
    getCallback().onConnectionFailed(t);
    scheduleConnectionRetry(null);
  }

  @Nonnull
  private Callback getCallback() {
    return Objects.requireNonNull(callback);
  }
}
