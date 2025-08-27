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
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SpanFromDiskExporter implements FromDiskExporter {

  private final FromDiskExporterImpl delegate;

  public static SpanFromDiskExporter create(SpanExporter exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<SpanData>builder(storage, SignalTypes.spans)
            .setExportFunction(exporter::export, SignalDeserializer.ofSpans())
            .build();
    return new SpanFromDiskExporter(delegate);
  }

  public static SpanFromDiskExporter create(HttpExporter<Marshaler> exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<SpanData>builder(storage, SignalTypes.spans)
            .setExporter(exporter)
            .build();
    return new SpanFromDiskExporter(delegate);
  }

  // Private because untested.
  @SuppressWarnings("unused")
  private static SpanFromDiskExporter create(GrpcExporter<Marshaler> exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<SpanData>builder(storage, SignalTypes.spans)
            .setExporter(exporter)
            .build();
    return new SpanFromDiskExporter(delegate);
  }

  private SpanFromDiskExporter(FromDiskExporterImpl delegate) {
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
