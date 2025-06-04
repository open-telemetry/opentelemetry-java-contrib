/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.service;

import io.opentelemetry.opamp.client.internal.connectivity.http.HttpErrorException;
import io.opentelemetry.opamp.client.internal.connectivity.http.HttpSender;
import io.opentelemetry.opamp.client.internal.connectivity.http.RetryAfterParser;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.delay.AcceptsDelaySuggestion;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicTaskExecutor;
import io.opentelemetry.opamp.client.internal.response.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import opamp.proto.AgentToServer;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;

public final class HttpRequestService implements RequestService, Runnable {
  private final HttpSender requestSender;
  private final PeriodicTaskExecutor executor;
  private final PeriodicDelay periodicRequestDelay;
  private final PeriodicDelay periodicRetryDelay;
  private final AtomicBoolean retryModeEnabled = new AtomicBoolean(false);
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final RetryAfterParser retryAfterParser;
  @Nullable private Callback callback;
  @Nullable private Supplier<Request> requestSupplier;
  public static final PeriodicDelay DEFAULT_DELAY_BETWEEN_REQUESTS =
      PeriodicDelay.ofFixedDuration(Duration.ofSeconds(30));

  /**
   * Creates an {@link HttpRequestService}.
   *
   * @param requestSender The HTTP sender implementation.
   */
  public static HttpRequestService create(HttpSender requestSender) {
    return create(requestSender, DEFAULT_DELAY_BETWEEN_REQUESTS, DEFAULT_DELAY_BETWEEN_REQUESTS);
  }

  /**
   * Creates an {@link HttpRequestService}.
   *
   * @param requestSender The HTTP sender implementation.
   * @param periodicRequestDelay The time to wait between requests in general.
   * @param periodicRetryDelay The time to wait between retries.
   */
  public static HttpRequestService create(
      HttpSender requestSender,
      PeriodicDelay periodicRequestDelay,
      PeriodicDelay periodicRetryDelay) {
    return new HttpRequestService(
        requestSender,
        PeriodicTaskExecutor.create(periodicRequestDelay),
        periodicRequestDelay,
        periodicRetryDelay,
        RetryAfterParser.getInstance());
  }

  HttpRequestService(
      HttpSender requestSender,
      PeriodicTaskExecutor executor,
      PeriodicDelay periodicRequestDelay,
      PeriodicDelay periodicRetryDelay,
      RetryAfterParser retryAfterParser) {
    this.requestSender = requestSender;
    this.executor = executor;
    this.periodicRequestDelay = periodicRequestDelay;
    this.periodicRetryDelay = periodicRetryDelay;
    this.retryAfterParser = retryAfterParser;
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    if (isRunning.compareAndSet(false, true)) {
      this.callback = callback;
      this.requestSupplier = requestSupplier;
      executor.start(this);
    } else {
      throw new IllegalStateException("RequestDispatcher is already running");
    }
  }

  @Override
  public void stop() {
    if (isRunning.compareAndSet(true, false)) {
      executor.executeNow();
      executor.stop();
    }
  }

  private void enableRetryMode(@Nullable Duration suggestedDelay) {
    if (retryModeEnabled.compareAndSet(false, true)) {
      if (suggestedDelay != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
        ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(suggestedDelay);
      }
      executor.setPeriodicDelay(periodicRetryDelay);
    }
  }

  private void disableRetryMode() {
    if (retryModeEnabled.compareAndSet(true, false)) {
      executor.setPeriodicDelay(periodicRequestDelay);
    }
  }

  @Override
  public void sendRequest() {
    if (!retryModeEnabled.get()) {
      executor.executeNow();
    }
  }

  @Override
  public void run() {
    doSendRequest();
  }

  private void doSendRequest() {
    try {
      AgentToServer agentToServer =
          Objects.requireNonNull(requestSupplier).get().getAgentToServer();

      byte[] data = agentToServer.encodeByteString().toByteArray();
      try (HttpSender.Response response =
          requestSender.send(new ByteArrayWriter(data), data.length).get()) {
        if (isSuccessful(response)) {
          handleSuccessResponse(
              Response.create(ServerToAgent.ADAPTER.decode(response.bodyInputStream())));
        } else {
          handleHttpError(response);
        }
      } catch (IOException e) {
        getCallback().onRequestFailed(e);
      }

    } catch (InterruptedException e) {
      getCallback().onRequestFailed(e);
    } catch (ExecutionException e) {
      if (e.getCause() != null) {
        getCallback().onRequestFailed(e.getCause());
      } else {
        getCallback().onRequestFailed(e);
      }
    }
  }

  private void handleHttpError(HttpSender.Response response) {
    int errorCode = response.statusCode();
    getCallback().onRequestFailed(new HttpErrorException(errorCode, response.statusMessage()));

    if (errorCode == 503 || errorCode == 429) {
      String retryAfterHeader = response.getHeader("Retry-After");
      Duration retryAfter = null;
      if (retryAfterHeader != null) {
        Optional<Duration> duration = retryAfterParser.tryParse(retryAfterHeader);
        if (duration.isPresent()) {
          retryAfter = duration.get();
        }
      }
      enableRetryMode(retryAfter);
    }
  }

  private static boolean isSuccessful(HttpSender.Response response) {
    return response.statusCode() >= 200 && response.statusCode() < 300;
  }

  private void handleSuccessResponse(Response response) {
    if (retryModeEnabled.get()) {
      disableRetryMode();
    }
    ServerToAgent serverToAgent = response.getServerToAgent();

    if (serverToAgent.error_response != null) {
      handleErrorResponse(serverToAgent.error_response);
    }

    getCallback().onRequestSuccess(response);
  }

  private void handleErrorResponse(ServerErrorResponse errorResponse) {
    if (errorResponse.type.equals(ServerErrorResponseType.ServerErrorResponseType_Unavailable)) {
      Duration retryAfter = null;
      if (errorResponse.retry_info != null) {
        retryAfter = Duration.ofNanos(errorResponse.retry_info.retry_after_nanoseconds);
      }
      enableRetryMode(retryAfter);
    }
  }

  private Callback getCallback() {
    return Objects.requireNonNull(callback);
  }

  private static class ByteArrayWriter implements Consumer<OutputStream> {
    private final byte[] data;

    private ByteArrayWriter(byte[] data) {
      this.data = data;
    }

    @SuppressWarnings("ThrowSpecificExceptions")
    @Override
    public void accept(OutputStream outputStream) {
      try {
        outputStream.write(data);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
