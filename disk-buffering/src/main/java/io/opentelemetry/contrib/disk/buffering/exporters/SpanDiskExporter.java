package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public final class SpanDiskExporter extends AbstractDiskExporter<SpanData> implements SpanExporter {
  private final SpanExporter wrapped;

  public SpanDiskExporter(SpanExporter wrapped, File rootDir, StorageConfiguration configuration) {
    super(rootDir, configuration);
    this.wrapped = wrapped;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return onExport(spans);
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      onShutDown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    } finally {
      wrapped.shutdown();
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  protected String getStorageFolderName() {
    return "spans";
  }

  @Override
  protected CompletableResultCode doExport(Collection<SpanData> data) {
    return wrapped.export(data);
  }

  @Override
  protected SignalSerializer<SpanData> getSerializer() {
    return SignalSerializer.ofSpans();
  }

  @Override
  public CompletableResultCode flush() {
    return wrapped.flush();
  }
}
