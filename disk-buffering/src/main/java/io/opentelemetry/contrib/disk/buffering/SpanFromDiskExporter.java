/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SpanFromDiskExporter implements FromDiskExporter {

  private final FromDiskExporterImpl<SpanData> delegate;

  public static SpanFromDiskExporter create(SpanExporter exporter, StorageConfiguration config)
      throws IOException {
    FromDiskExporterImpl<SpanData> delegate =
        FromDiskExporterImpl.<SpanData>builder()
            .setFolderName(SignalTypes.spans.name())
            .setStorageConfiguration(config)
            .setDeserializer(SignalDeserializer.ofSpans())
            .setExportFunction(exporter::export)
            .setDebugEnabled(config.isDebugEnabled())
            .build();
    return new SpanFromDiskExporter(delegate);
  }

  private SpanFromDiskExporter(FromDiskExporterImpl<SpanData> delegate) {
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
