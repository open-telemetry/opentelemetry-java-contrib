/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.time.Duration;
import java.util.Collection;
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
    CompletableResultCode result =
        storage.write(items).join(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);

    if (!result.isDone()) {
      TimeoutException timeout =
          new TimeoutException("Storage write timed out after " + writeTimeout.toMillis() + "ms");
      callback.onExportError(items, timeout);
      return CompletableResultCode.ofExceptionalFailure(timeout);
    }

    if (result.isSuccess()) {
      callback.onExportSuccess(items);
      return CompletableResultCode.ofSuccess();
    }

    Throwable error = result.getFailureThrowable();
    callback.onExportError(items, error);
    if (error != null) {
      return CompletableResultCode.ofExceptionalFailure(error);
    }
    return CompletableResultCode.ofFailure();
  }
}
