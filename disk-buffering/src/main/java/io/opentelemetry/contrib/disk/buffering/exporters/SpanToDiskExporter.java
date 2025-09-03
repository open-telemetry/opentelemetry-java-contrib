/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.SignalStorageExporter;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Collection;

/** Exporter that stores spans into disk. */
public final class SpanToDiskExporter implements SpanExporter {

  private final SignalStorageExporter<SpanData> storageExporter;
  private final ExporterCallback<SpanData> callback;

  private SpanToDiskExporter(
      SignalStorageExporter<SpanData> storageExporter, ExporterCallback<SpanData> callback) {
    this.storageExporter = storageExporter;
    this.callback = callback;
  }

  public static Builder builder(SignalStorage.Span storage) {
    return new Builder(storage);
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return storageExporter.exportToStorage(spans);
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
    private final SignalStorage.Span storage;
    private ExporterCallback<SpanData> callback = ExporterCallback.noop();
    private Duration writeTimeout = Duration.ofSeconds(10);

    private Builder(SignalStorage.Span storage) {
      this.storage = storage;
    }

    @CanIgnoreReturnValue
    public Builder setExporterCallback(ExporterCallback<SpanData> value) {
      callback = value;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setWriteTimeout(Duration value) {
      writeTimeout = value;
      return this;
    }

    public SpanToDiskExporter build() {
      SignalStorageExporter<SpanData> storageExporter =
          new SignalStorageExporter<>(storage, callback, writeTimeout);
      return new SpanToDiskExporter(storageExporter, callback);
    }
  }
}
