/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.opentelemetry.contrib.disk.buffering.SignalType;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/** Internal utility for common export to disk operations across all exporters. */
final class SignalStorageExporter<T> {
  private final SignalStorage<T> storage;
  private final ExporterCallback callback;
  private final Duration writeTimeout;
  private final SignalType type;

  public SignalStorageExporter(
      SignalStorage<T> storage, ExporterCallback callback, Duration writeTimeout, SignalType type) {
    this.storage = storage;
    this.callback = callback;
    this.writeTimeout = writeTimeout;
    this.type = type;
  }

  public CompletableResultCode exportToStorage(Collection<T> items) {
    CompletableFuture<WriteResult> future = storage.write(items);
    try {
      WriteResult operation = future.get(writeTimeout.toMillis(), MILLISECONDS);
      if (operation.isSuccessful()) {
        callback.onExportSuccess(type);
        return CompletableResultCode.ofSuccess();
      }

      Throwable error = operation.getError();
      callback.onExportError(type, error);
      if (error != null) {
        return CompletableResultCode.ofExceptionalFailure(error);
      }
      return CompletableResultCode.ofFailure();
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      callback.onExportError(type, e);
      return CompletableResultCode.ofExceptionalFailure(e);
    }
  }
}
