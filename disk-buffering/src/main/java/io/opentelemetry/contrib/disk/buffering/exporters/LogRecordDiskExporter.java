package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public final class LogRecordDiskExporter extends AbstractDiskExporter<LogRecordData>
    implements LogRecordExporter {
  private final LogRecordExporter wrapped;

  public LogRecordDiskExporter(
      LogRecordExporter wrapped, File rootDir, StorageConfiguration configuration) {
    super(rootDir, configuration);
    this.wrapped = wrapped;
  }

  @Override
  protected String getStorageFolderName() {
    return "logs";
  }

  @Override
  protected CompletableResultCode doExport(Collection<LogRecordData> logRecordData) {
    return wrapped.export(logRecordData);
  }

  @Override
  protected SignalSerializer<LogRecordData> getSerializer() {
    return SignalSerializer.ofLogs();
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    return onExport(logs);
  }

  @Override
  public CompletableResultCode flush() {
    return wrapped.flush();
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
}
