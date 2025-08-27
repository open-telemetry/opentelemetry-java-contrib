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
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MetricFromDiskExporter implements FromDiskExporter {

  private final FromDiskExporterImpl delegate;

  public static MetricFromDiskExporter create(MetricExporter exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<MetricData>builder(storage, SignalTypes.metrics)
            .setExportFunction(exporter::export, SignalDeserializer.ofMetrics())
            .build();
    return new MetricFromDiskExporter(delegate);
  }

  public static MetricFromDiskExporter create(HttpExporter<Marshaler> exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<MetricData>builder(storage, SignalTypes.metrics)
            .setExporter(exporter)
            .build();
    return new MetricFromDiskExporter(delegate);
  }

  // Private because untested.
  @SuppressWarnings("unused")
  private static MetricFromDiskExporter create(GrpcExporter<Marshaler> exporter, Storage storage)
      throws IOException {
    FromDiskExporterImpl delegate =
        FromDiskExporterImpl.<MetricData>builder(storage, SignalTypes.metrics)
            .setExporter(exporter)
            .build();
    return new MetricFromDiskExporter(delegate);
  }

  private MetricFromDiskExporter(FromDiskExporterImpl delegate) {
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
