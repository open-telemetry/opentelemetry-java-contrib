/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MetricFromDiskExporter implements FromDiskExporter {

  private final FromDiskExporterImpl<MetricData> delegate;

  public static MetricFromDiskExporter create(MetricExporter exporter, StorageConfiguration config)
      throws IOException {
    FromDiskExporterImpl<MetricData> delegate =
        FromDiskExporterImpl.<MetricData>builder()
            .setFolderName("metrics")
            .setStorageConfiguration(config)
            .setDeserializer(SignalDeserializer.ofMetrics())
            .setExportFunction(exporter::export)
            .build();
    return new MetricFromDiskExporter(delegate);
  }

  private MetricFromDiskExporter(FromDiskExporterImpl<MetricData> delegate) {
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
