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
import io.opentelemetry.opamp.client.internal.request.delay.RetryPeriodicDelay;
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseException;
import io.opentelemetry.opamp.client.internal.response.Response;
import io.opentelemetry.opamp.client.request.service.RequestService;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import opamp.proto.AgentToServer;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerErrorResponseType;
import opamp.proto.ServerToAgent;

public final class HttpRequestService implements RequestService {
  private final HttpSender requestSender;
  // must be a single threaded executor, the code in this class relies on requests being processed
  // serially
  private final ScheduledExecutorService executorService;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final AtomicBoolean hasStopped = new AtomicBoolean(false);
  private final ConnectionStatus connectionStatus;
  private final AtomicReference<ScheduledFuture<?>> scheduledTask = new AtomicReference<>();
  private final RetryAfterParser retryAfterParser;
  @Nullable private Callback callback;
  @Nullable private Supplier<Request> requestSupplier;
  public static final PeriodicDelay DEFAULT_DELAY_BETWEEN_REQUESTS =
      PeriodicDelay.ofFixedDuration(Duration.ofSeconds(30));
  public static final PeriodicDelay DEFAULT_DELAY_BETWEEN_RETRIES =
      RetryPeriodicDelay.create(Duration.ofSeconds(30));

  /**
   * Creates an {@link HttpRequestService}.
   *
   * @param requestSender The HTTP sender implementation.
   */
  public static HttpRequestService create(HttpSender requestSender) {
    return create(requestSender, DEFAULT_DELAY_BETWEEN_REQUESTS, DEFAULT_DELAY_BETWEEN_RETRIES);
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
    this.retryAfterParser = retryAfterParser;
    this.connectionStatus = new ConnectionStatus(periodicRequestDelay, periodicRetryDelay);
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    if (hasStopped.get()) {
      throw new IllegalStateException("HttpRequestService cannot start after it has been stopped.");
    }
    if (isRunning.compareAndSet(false, true)) {
      this.callback = callback;
      this.requestSupplier = requestSupplier;
      scheduleNextExecution();
    } else {
      throw new IllegalStateException("HttpRequestService is already running");
    }
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

    executorService.execute(
        () -> {
          // cancel the already scheduled task, a new one is created after current request is
          // processed
          ScheduledFuture<?> scheduledFuture = scheduledTask.get();
          if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
          }
          sendAndScheduleNext();
        });
  }

  private void sendAndScheduleNext() {
    doSendRequest();
    scheduleNextExecution();
  }

  private void scheduleNextExecution() {
    scheduledTask.set(
        executorService.schedule(
            this::sendAndScheduleNext,
            connectionStatus.getNextDelay().toNanos(),
            TimeUnit.NANOSECONDS));
  }

  private void doSendRequest() {
    AgentToServer agentToServer = Objects.requireNonNull(requestSupplier).get().getAgentToServer();

    byte[] data = agentToServer.encodeByteString().toByteArray();
    CompletableFuture<HttpSender.Response> future =
        requestSender.send(outputStream -> outputStream.write(data), data.length);
    try (HttpSender.Response response = future.get(30, TimeUnit.SECONDS)) {
      getCallback().onConnectionSuccess();
      if (isSuccessful(response)) {
        handleHttpSuccess(
            Response.create(ServerToAgent.ADAPTER.decode(response.bodyInputStream())));
      } else {
        handleHttpError(response);
      }
    } catch (IOException | InterruptedException | TimeoutException e) {
      getCallback().onConnectionFailed(e);
      connectionStatus.retryAfter(null);
    } catch (ExecutionException e) {
      if (e.getCause() != null) {
        getCallback().onConnectionFailed(e.getCause());
      } else {
        getCallback().onConnectionFailed(e);
      }
      connectionStatus.retryAfter(null);
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
      connectionStatus.retryAfter(retryAfter);
    }
  }

  private static boolean isSuccessful(HttpSender.Response response) {
    return response.statusCode() >= 200 && response.statusCode() < 300;
  }

  private void handleHttpSuccess(Response response) {
    connectionStatus.success();
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
      connectionStatus.retryAfter(retryAfter);
    }
    getCallback()
        .onRequestFailed(
            new OpampServerResponseException(errorResponse, errorResponse.error_message));
  }

  private Callback getCallback() {
    return Objects.requireNonNull(callback);
  }

  // this class is only used from a single threaded ScheduledExecutorService, hence no
  // synchronization is needed
  private static class ConnectionStatus {
    private final PeriodicDelay periodicRequestDelay;
    private final PeriodicDelay periodicRetryDelay;

    private boolean retrying;
    private PeriodicDelay currentDelay;

    ConnectionStatus(PeriodicDelay periodicRequestDelay, PeriodicDelay periodicRetryDelay) {
      this.periodicRequestDelay = periodicRequestDelay;
      this.periodicRetryDelay = periodicRetryDelay;
      currentDelay = periodicRequestDelay;
    }

    void success() {
      // after successful request transition from retry to regular delay
      if (retrying) {
        retrying = false;
        periodicRequestDelay.reset();
        currentDelay = periodicRequestDelay;
      }
    }

    void retryAfter(@Nullable Duration retryAfter) {
      // after failed request transition from regular to retry delay
      if (!retrying) {
        retrying = true;
        periodicRetryDelay.reset();
        currentDelay = periodicRetryDelay;
        if (retryAfter != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
          ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(retryAfter);
        }
      }
    }

    Duration getNextDelay() {
      return currentDelay.getNextDelay();
    }
  }
}
