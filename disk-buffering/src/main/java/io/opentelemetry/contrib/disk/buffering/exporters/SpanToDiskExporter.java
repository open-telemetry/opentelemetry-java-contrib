package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.SignalType;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Collection;

public final class SpanToDiskExporter implements SpanExporter {
  private final SignalStorageExporter<SpanData> storageExporter;
  private final ExporterCallback callback;
  private static final SignalType TYPE = SignalType.SPAN;

  private SpanToDiskExporter(
      SignalStorageExporter<SpanData> storageExporter, ExporterCallback callback) {
    this.storageExporter = storageExporter;
    this.callback = callback;
  }

  public Builder builder(SignalStorage.Span storage) {
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
    callback.onShutdown(TYPE);
    return CompletableResultCode.ofSuccess();
  }

  public static final class Builder {
    private final SignalStorage.Span storage;
    private ExporterCallback callback = ExporterCallback.noop();
    private Duration writeTimeout = Duration.ofSeconds(10);

    public Builder setExporterCallback(ExporterCallback value) {
      callback = value;
      return this;
    }

    public Builder setWriteTimeout(Duration value) {
      writeTimeout = value;
      return this;
    }

    public SpanToDiskExporter build() {
      SignalStorageExporter<SpanData> storageExporter =
          new SignalStorageExporter<>(storage, callback, writeTimeout, TYPE);
      return new SpanToDiskExporter(storageExporter, callback);
    }

    private Builder(SignalStorage.Span storage) {
      this.storage = storage;
    }
  }
}
