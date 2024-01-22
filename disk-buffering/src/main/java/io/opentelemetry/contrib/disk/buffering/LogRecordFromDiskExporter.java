/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LogRecordFromDiskExporter implements FromDiskExporter {

  private final FromDiskExporterImpl<LogRecordData> delegate;

  public static LogRecordFromDiskExporter create(
      LogRecordExporter exporter, StorageConfiguration config) throws IOException {
    FromDiskExporterImpl<LogRecordData> delegate =
        FromDiskExporterImpl.<LogRecordData>builder()
            .setFolderName("logs")
            .setStorageConfiguration(config)
            .setDeserializer(SignalSerializer.ofLogs())
            .setExportFunction(exporter::export)
            .build();
    return new LogRecordFromDiskExporter(delegate);
  }

  private LogRecordFromDiskExporter(FromDiskExporterImpl<LogRecordData> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    return delegate.exportStoredBatch(timeout, unit);
  }

  @Override
  public void shutdown() throws IOException {
    delegate.shutdown();
  }
}
