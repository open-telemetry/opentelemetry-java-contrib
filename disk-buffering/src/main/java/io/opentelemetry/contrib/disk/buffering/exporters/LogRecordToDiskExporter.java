/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.exporters.callback.NoopExporterCallback;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.SignalStorageExporter;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.time.Duration;
import java.util.Collection;

/** Exporter that stores logs into disk. */
public final class LogRecordToDiskExporter implements LogRecordExporter {
  private final SignalStorageExporter<LogRecordData> storageExporter;
  private final ExporterCallback<LogRecordData> callback;
  private static final ExporterCallback<LogRecordData> DEFAULT_CALLBACK =
      new NoopExporterCallback<>();
  private static final Duration DEFAULT_EXPORT_TIMEOUT = Duration.ofSeconds(10);

  private LogRecordToDiskExporter(
      SignalStorageExporter<LogRecordData> storageExporter,
      ExporterCallback<LogRecordData> callback) {
    this.storageExporter = storageExporter;
    this.callback = callback;
  }

  public static Builder builder(SignalStorage.LogRecord storage) {
    return new Builder(storage);
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    return storageExporter.exportToStorage(logs);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    callback.onShutdown();
    return CompletableResultCode.ofSuccess();
  }

  public static final class Builder {
    private final SignalStorage.LogRecord storage;
    private ExporterCallback<LogRecordData> callback = DEFAULT_CALLBACK;
    private Duration writeTimeout = DEFAULT_EXPORT_TIMEOUT;

    @CanIgnoreReturnValue
    public Builder setExporterCallback(ExporterCallback<LogRecordData> value) {
      callback = value;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setWriteTimeout(Duration value) {
      writeTimeout = value;
      return this;
    }

    public LogRecordToDiskExporter build() {
      SignalStorageExporter<LogRecordData> storageExporter =
          new SignalStorageExporter<>(storage, callback, writeTimeout);
      return new LogRecordToDiskExporter(storageExporter, callback);
    }

    private Builder(SignalStorage.LogRecord storage) {
      this.storage = storage;
    }
  }
}
