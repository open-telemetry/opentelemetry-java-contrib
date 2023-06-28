/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.exporters.DiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This is a {@link MetricExporter} wrapper that takes care of intercepting all the signals sent out
 * to be exported, tries to store them in the disk in order to export them later.
 *
 * <p>In order to use it, you need to wrap your own {@link MetricExporter} with a new instance of
 * this one, which will be the one you need to register in your {@link MetricReader}.
 */
public final class MetricDiskExporter implements MetricExporter, StoredBatchExporter {
  private final MetricExporter wrapped;
  private final DiskExporter<MetricData> diskExporter;
  /**
   * @param wrapped - Your own exporter.
   * @param rootDir - The directory to create this signal's cache dir where all the data will be
   *     written into.
   * @param configuration - How you want to manage the storage process.
   */
  public MetricDiskExporter(
      MetricExporter wrapped, File rootDir, StorageConfiguration configuration) {
    this.wrapped = wrapped;
    diskExporter =
        new DiskExporter<>(
            rootDir, configuration, "metrics", SignalSerializer.ofMetrics(), wrapped::export);
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    return diskExporter.onExport(metrics);
  }

  @Override
  public CompletableResultCode flush() {
    return wrapped.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      diskExporter.onShutDown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    } finally {
      wrapped.shutdown();
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return wrapped.getAggregationTemporality(instrumentType);
  }

  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    return diskExporter.exportStoredBatch(timeout, unit);
  }
}
