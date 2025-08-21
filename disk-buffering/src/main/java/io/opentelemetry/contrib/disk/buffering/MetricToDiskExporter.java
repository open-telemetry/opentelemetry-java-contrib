/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.Collection;

/**
 * This class implements a {@link MetricExporter} that delegates to an instance of {@code
 * ToDiskExporter<MetricData>}.
 */
public class MetricToDiskExporter implements MetricExporter {

  private final ToDiskExporter<MetricData> delegate;
  private final AggregationTemporalitySelector aggregationTemporalitySelector;

  /**
   * Creates a new MetricToDiskExporter that will buffer Metric telemetry on disk storage.
   *
   * @param delegate - The MetricExporter to delegate to if disk writing fails.
   * @param storage - The Storage instance that specifies how storage is managed.
   * @return A new MetricToDiskExporter instance.
   */
  public static MetricToDiskExporter create(MetricExporter delegate, Storage<MetricData> storage) {
    ToDiskExporter<MetricData> toDisk =
        ToDiskExporter.builder(storage)
            .setSerializer(SignalSerializer.ofMetrics())
            .setExportFunction(delegate::export)
            .build();
    return new MetricToDiskExporter(toDisk, delegate);
  }

  // VisibleForTesting
  MetricToDiskExporter(
      ToDiskExporter<MetricData> delegate, AggregationTemporalitySelector selector) {
    this.delegate = delegate;
    this.aggregationTemporalitySelector = selector;
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    return delegate.export(metrics);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      delegate.shutdown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return aggregationTemporalitySelector.getAggregationTemporality(instrumentType);
  }
}
