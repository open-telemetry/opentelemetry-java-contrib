/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Internal utility for common export to disk operations across all exporters. */
public final class SignalStorageExporter<T> {
  private final SignalStorage<T> storage;
  private final ExporterCallback<T> callback;
  private final Duration writeTimeout;

  public SignalStorageExporter(
      SignalStorage<T> storage, ExporterCallback<T> callback, Duration writeTimeout) {
    this.storage = storage;
    this.callback = callback;
    this.writeTimeout = writeTimeout;
  }

  public CompletableResultCode exportToStorage(Collection<T> items) {
    CompletableFuture<WriteResult> future = storage.write(items);
    try {
      WriteResult operation = future.get(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);
      if (operation.isSuccessful()) {
        callback.onExportSuccess(items);
        return CompletableResultCode.ofSuccess();
      }

      Throwable error = operation.getError();
      callback.onExportError(items, error);
      if (error != null) {
        return CompletableResultCode.ofExceptionalFailure(error);
      }
      return CompletableResultCode.ofFailure();
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      callback.onExportError(items, e);
      return CompletableResultCode.ofExceptionalFailure(e);
    }
  }
}
