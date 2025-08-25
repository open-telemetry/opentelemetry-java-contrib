/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.SignalStorageExporter;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.time.Duration;
import java.util.Collection;

/** Exporter that stores metrics into disk. */
public final class MetricToDiskExporter implements MetricExporter {
  private final SignalStorageExporter<MetricData> storageExporter;
  private final AggregationTemporalitySelector aggregationTemporalitySelector;
  private final ExporterCallback<MetricData> callback;

  private MetricToDiskExporter(
      SignalStorageExporter<MetricData> storageExporter,
      AggregationTemporalitySelector aggregationTemporalitySelector,
      ExporterCallback<MetricData> callback) {
    this.storageExporter = storageExporter;
    this.aggregationTemporalitySelector = aggregationTemporalitySelector;
    this.callback = callback;
  }

  public static Builder builder(SignalStorage.Metric storage) {
    return new Builder(storage);
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    return storageExporter.exportToStorage(metrics);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    callback.onShutdown();
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return aggregationTemporalitySelector.getAggregationTemporality(instrumentType);
  }

  public static final class Builder {
    private final SignalStorage.Metric storage;
    private AggregationTemporalitySelector aggregationTemporalitySelector =
        AggregationTemporalitySelector.alwaysCumulative();
    private ExporterCallback<MetricData> callback = ExporterCallback.noop();
    private Duration writeTimeout = Duration.ofSeconds(10);

    @CanIgnoreReturnValue
    public Builder setExporterCallback(ExporterCallback<MetricData> value) {
      callback = value;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setWriteTimeout(Duration value) {
      writeTimeout = value;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setAggregationTemporalitySelector(AggregationTemporalitySelector value) {
      aggregationTemporalitySelector = value;
      return this;
    }

    public MetricToDiskExporter build() {
      SignalStorageExporter<MetricData> storageExporter =
          new SignalStorageExporter<>(storage, callback, writeTimeout);
      return new MetricToDiskExporter(storageExporter, aggregationTemporalitySelector, callback);
    }

    private Builder(SignalStorage.Metric storage) {
      this.storage = storage;
    }
  }
}
