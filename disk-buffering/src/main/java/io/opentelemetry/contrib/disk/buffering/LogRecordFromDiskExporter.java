/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LogRecordFromDiskExporter implements FromDiskExporter {

  private final FromDiskExporterImpl delegate;

  public static LogRecordFromDiskExporter create(LogRecordExporter exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<LogRecordData>builder(storage, SignalTypes.logs)
            .setExportFunction(exporter::export, SignalDeserializer.ofLogs())
            .build();
    return new LogRecordFromDiskExporter(delegate);
  }

  public static LogRecordFromDiskExporter create(HttpExporter<Marshaler> exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<LogRecordData>builder(storage, SignalTypes.logs)
            .setExporter(exporter)
            .build();
    return new LogRecordFromDiskExporter(delegate);
  }

  // Private because untested.
  @SuppressWarnings("unused")
  private static LogRecordFromDiskExporter create(GrpcExporter<Marshaler> exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<LogRecordData>builder(storage, SignalTypes.logs)
            .setExporter(exporter)
            .build();
    return new LogRecordFromDiskExporter(delegate);
  }

  private LogRecordFromDiskExporter(FromDiskExporterImpl delegate) {
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
