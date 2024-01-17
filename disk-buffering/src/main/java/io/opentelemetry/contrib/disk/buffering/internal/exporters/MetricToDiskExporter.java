/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

/**
 * This class implements a {@link MetricExporter} that delegates to an instance of {@code
 * ToDiskExporter<MetricData>}.
 */
public class MetricToDiskExporter implements MetricExporter {

  private final ToDiskExporter<MetricData> delegate;
  private final Function<InstrumentType, AggregationTemporality> typeToTemporality;

  public MetricToDiskExporter(
      ToDiskExporter<MetricData> delegate,
      Function<InstrumentType, AggregationTemporality> typeToTemporality) {
    this.delegate = delegate;
    this.typeToTemporality = typeToTemporality;
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
    return typeToTemporality.apply(instrumentType);
  }
}
