package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.exporters.AbstractDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public final class MetricDiskExporter extends AbstractDiskExporter<MetricData>
    implements MetricExporter {
  private final MetricExporter wrapped;

  public MetricDiskExporter(
      MetricExporter wrapped, File rootDir, StorageConfiguration configuration) {
    super(rootDir, configuration);
    this.wrapped = wrapped;
  }

  @Override
  protected String getStorageFolderName() {
    return "metrics";
  }

  @Override
  protected CompletableResultCode doExport(Collection<MetricData> metricData) {
    return wrapped.export(metricData);
  }

  @Override
  protected SignalSerializer<MetricData> getSerializer() {
    return SignalSerializer.ofMetrics();
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    return onExport(metrics);
  }

  @Override
  public CompletableResultCode flush() {
    return wrapped.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      onShutDown();
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
}
