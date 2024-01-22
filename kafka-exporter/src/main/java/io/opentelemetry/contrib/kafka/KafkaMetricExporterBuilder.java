/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.kafka.impl.KafkaExporterBuilder;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Builder utility for {@link KafkaMetricExporter}.
 *
 * @since 1.27.0
 */
public final class KafkaMetricExporterBuilder {

  private final KafkaExporterBuilder<MetricsRequestMarshaler> delegate;

  private AggregationTemporalitySelector aggregationTemporalitySelector =
      AggregationTemporalitySelector.alwaysCumulative();

  private DefaultAggregationSelector defaultAggregationSelector =
      DefaultAggregationSelector.getDefault();

  KafkaMetricExporterBuilder(KafkaExporterBuilder<MetricsRequestMarshaler> delegate) {
    this.delegate = delegate;
  }

  KafkaMetricExporterBuilder() {
    this(new KafkaExporterBuilder<>("kafka", "metric"));
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setProtocolVersion(String protocolVersion) {
    requireNonNull(protocolVersion, "protocolVersion");
    delegate.setProtocolVersion(protocolVersion);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setBrokers(String brokers) {
    requireNonNull(brokers, "brokers");
    delegate.setBrokers(brokers);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setTopic(String topic) {
    requireNonNull(topic, "topic");
    delegate.setTopic(topic);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setTimeout(long timeout, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(timeout >= 0, "timeout must be non-negative");
    delegate.setTimeout(timeout, unit);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setTimeout(Duration timeout) {
    requireNonNull(timeout, "timeout");
    return setTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setRetryPolicy(RetryPolicy retryPolicy) {
    requireNonNull(retryPolicy, "retryPolicy");
    delegate.setRetryPolicy(retryPolicy);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setMeterProvider(MeterProvider meterProvider) {
    requireNonNull(meterProvider, "meterProvider");
    setMeterProvider(() -> meterProvider);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setMeterProvider(
      Supplier<MeterProvider> meterProviderSupplier) {
    requireNonNull(meterProviderSupplier, "meterProviderSupplier");
    delegate.setMeterProvider(meterProviderSupplier);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setAggregationTemporalitySelector(
      AggregationTemporalitySelector aggregationTemporalitySelector) {
    requireNonNull(aggregationTemporalitySelector, "aggregationTemporalitySelector");
    this.aggregationTemporalitySelector = aggregationTemporalitySelector;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaMetricExporterBuilder setDefaultAggregationSelector(
      DefaultAggregationSelector defaultAggregationSelector) {
    requireNonNull(defaultAggregationSelector, "defaultAggregationSelector");
    this.defaultAggregationSelector = defaultAggregationSelector;
    return this;
  }

  public KafkaMetricExporter build() {
    return new KafkaMetricExporter(
        delegate, delegate.build(), aggregationTemporalitySelector, defaultAggregationSelector);
  }
}
