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
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseError;
import io.opentelemetry.opamp.client.internal.response.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import opamp.proto.AgentToServer;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;

public final class HttpRequestService implements RequestService {
  private final HttpSender requestSender;
  private final ScheduledExecutorService executorService;
  private final PeriodicDelay periodicRequestDelay;
  private final PeriodicDelay periodicRetryDelay;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final AtomicBoolean hasStopped = new AtomicBoolean(false);
  private final AtomicReference<PeriodicDelay> currentDelay;
  private final AtomicReference<ScheduledFuture<?>> currentTask = new AtomicReference<>();
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
        Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory()),
        periodicRequestDelay,
        periodicRetryDelay,
        RetryAfterParser.getInstance());
  }

  HttpRequestService(
      HttpSender requestSender,
      ScheduledExecutorService executorService,
      PeriodicDelay periodicRequestDelay,
      PeriodicDelay periodicRetryDelay,
      RetryAfterParser retryAfterParser) {
    this.requestSender = requestSender;
    this.executorService = executorService;
    this.periodicRequestDelay = periodicRequestDelay;
    this.periodicRetryDelay = periodicRetryDelay;
    this.retryAfterParser = retryAfterParser;
    currentDelay = new AtomicReference<>(periodicRequestDelay);
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    if (hasStopped.get()) {
      throw new IllegalStateException("HttpRequestService cannot start after it has been stopped.");
    }
    if (isRunning.compareAndSet(false, true)) {
      this.callback = callback;
      this.requestSupplier = requestSupplier;
      currentTask.set(
          executorService.schedule(
              this::periodicSend, getNextDelay().toNanos(), TimeUnit.NANOSECONDS));
    } else {
      throw new IllegalStateException("HttpRequestService is already running");
    }
  }

  private void periodicSend() {
    doSendRequest();
    // schedule the next execution
    currentTask.set(
        executorService.schedule(
            this::periodicSend, getNextDelay().toNanos(), TimeUnit.NANOSECONDS));
  }

  private void sendOnce() {
    executorService.execute(this::doSendRequest);
  }

  private Duration getNextDelay() {
    return Objects.requireNonNull(currentDelay.get()).getNextDelay();
  }

  @Override
  public void stop() {
    if (isRunning.compareAndSet(true, false)) {
      hasStopped.set(true);
      executorService.shutdown();
    }
  }

  @Override
  public void sendRequest() {
    if (!isRunning.get()) {
      throw new IllegalStateException("HttpRequestService is not running");
    }

    sendOnce();
  }

  private void doSendRequest() {
    AgentToServer agentToServer = Objects.requireNonNull(requestSupplier).get().getAgentToServer();

    byte[] data = agentToServer.encodeByteString().toByteArray();
    CompletableFuture<HttpSender.Response> future =
        requestSender.send(outputStream -> outputStream.write(data), data.length);
    try (HttpSender.Response response = future.get(30, TimeUnit.SECONDS)) {
      getCallback().onConnectionSuccess();
      if (isSuccessful(response)) {
        handleSuccessResponse(
            Response.create(ServerToAgent.ADAPTER.decode(response.bodyInputStream())));
      } else {
        handleHttpError(response);
      }
    } catch (IOException | InterruptedException | TimeoutException e) {
      getCallback().onConnectionFailed(e);
    } catch (ExecutionException e) {
      if (e.getCause() != null) {
        getCallback().onConnectionFailed(e.getCause());
      } else {
        getCallback().onConnectionFailed(e);
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
      useRetryDelay(retryAfter);
    }
  }

  private static boolean isSuccessful(HttpSender.Response response) {
    return response.statusCode() >= 200 && response.statusCode() < 300;
  }

  private void handleSuccessResponse(Response response) {
    useRegularDelay();
    ServerToAgent serverToAgent = response.getServerToAgent();

    if (serverToAgent.error_response != null) {
      handleErrorResponse(serverToAgent.error_response);
    } else {
      getCallback().onRequestSuccess(response);
    }
  }

  private void handleErrorResponse(ServerErrorResponse errorResponse) {
    if (errorResponse.type.equals(ServerErrorResponseType.ServerErrorResponseType_Unavailable)) {
      Duration retryAfter = null;
      if (errorResponse.retry_info != null) {
        retryAfter = Duration.ofNanos(errorResponse.retry_info.retry_after_nanoseconds);
      }
      useRetryDelay(retryAfter);
    }
    getCallback().onRequestFailed(new OpampServerResponseError(errorResponse.error_message));
  }

  private void useRegularDelay() {
    if (currentDelay.compareAndSet(periodicRetryDelay, periodicRequestDelay)) {
      cancelCurrentTask();
      periodicRequestDelay.reset();
    }
  }

  private void useRetryDelay(@Nullable Duration retryAfter) {
    if (currentDelay.compareAndSet(periodicRequestDelay, periodicRetryDelay)) {
      cancelCurrentTask();
      periodicRetryDelay.reset();
      if (retryAfter != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
        ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(retryAfter);
      }
    }
  }

  private void cancelCurrentTask() {
    ScheduledFuture<?> future = currentTask.get();
    if (future != null) {
      future.cancel(false);
    }
  }

  private Callback getCallback() {
    return Objects.requireNonNull(callback);
  }

  private static class DaemonThreadFactory implements ThreadFactory {
    private final ThreadFactory delegate = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(@Nonnull Runnable r) {
      Thread t = delegate.newThread(r);
      try {
        t.setDaemon(true);
      } catch (SecurityException e) {
        // Well, we tried.
      }
      return t;
    }
  }
}
