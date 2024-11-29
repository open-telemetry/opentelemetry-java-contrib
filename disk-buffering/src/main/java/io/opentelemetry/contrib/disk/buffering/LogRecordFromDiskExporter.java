/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
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
            .setFolderName(SignalTypes.logs.name())
            .setStorageConfiguration(config)
            .setDeserializer(SignalDeserializer.ofLogs())
            .setExportFunction(exporter::export)
            .setDebugEnabled(config.isDebugEnabled())
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
