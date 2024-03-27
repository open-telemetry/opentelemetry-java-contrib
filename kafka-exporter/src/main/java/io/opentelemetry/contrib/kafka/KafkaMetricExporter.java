/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import io.opentelemetry.contrib.kafka.impl.KafkaExporter;
import io.opentelemetry.contrib.kafka.impl.KafkaExporterBuilder;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A {@link MetricExporter} which writes {@linkplain MetricData spans} to Kafka in OTLP format. */
public final class KafkaMetricExporter implements MetricExporter {

  private static final Logger logger = Logger.getLogger(KafkaMetricExporter.class.getName());

  private final AtomicBoolean isShutdown = new AtomicBoolean();

  private final KafkaExporterBuilder<MetricsRequestMarshaler> builder;

  private final KafkaExporter<MetricsRequestMarshaler> delegate;

  private final AggregationTemporalitySelector aggregationTemporalitySelector;

  private final DefaultAggregationSelector defaultAggregationSelector;

  /**
   * Returns a new {@link KafkaMetricExporter} using the default values.
   *
   * <p>To load configuration values from environment variables and system properties, use <a
   * href="https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure">opentelemetry-sdk-extension-autoconfigure</a>.
   *
   * @return a new {@link KafkaMetricExporter} instance.
   */
  public static KafkaMetricExporter getDefault() {
    return builder().build();
  }

  /**
   * Returns a new builder instance for this exporter.
   *
   * @return a new builder instance for this exporter.
   */
  public static KafkaMetricExporterBuilder builder() {
    return new KafkaMetricExporterBuilder();
  }

  KafkaMetricExporter(
      KafkaExporterBuilder<MetricsRequestMarshaler> builder,
      KafkaExporter<MetricsRequestMarshaler> delegate,
      AggregationTemporalitySelector aggregationTemporalitySelector,
      DefaultAggregationSelector defaultAggregationSelector) {
    this.builder = builder;
    this.delegate = delegate;
    this.aggregationTemporalitySelector = aggregationTemporalitySelector;
    this.defaultAggregationSelector = defaultAggregationSelector;
  }

  public KafkaMetricExporterBuilder toBuilder() {
    return new KafkaMetricExporterBuilder(builder.copy());
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return aggregationTemporalitySelector.getAggregationTemporality(instrumentType);
  }

  @Override
  public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
    return defaultAggregationSelector.getDefaultAggregation(instrumentType);
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    if (isShutdown.get()) {
      return CompletableResultCode.ofFailure();
    }
    try {
      MetricsRequestMarshaler exportRequest = MetricsRequestMarshaler.create(metrics);
      return delegate.export(exportRequest, metrics.size());
    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "send metrics to kafka failed.", e);
      return CompletableResultCode.ofFailure();
    }
  }

  @Override
  public CompletableResultCode flush() {
    // TODO
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      logger.log(Level.INFO, "Calling shutdown() multiple times.");
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public String toString() {
    return "KafkaMetricExporter{" + builder.toString(false) + "}";
  }
}
