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
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Builder utility for {@link KafkaLogRecordExporter}.
 *
 * @since 1.27.0
 */
public final class KafkaLogRecordExporterBuilder {

  private final KafkaExporterBuilder<LogsRequestMarshaler> delegate;

  KafkaLogRecordExporterBuilder(KafkaExporterBuilder<LogsRequestMarshaler> delegate) {
    this.delegate = delegate;
  }

  KafkaLogRecordExporterBuilder() {
    this(new KafkaExporterBuilder<>("kafka", "log"));
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setProtocolVersion(String protocolVersion) {
    requireNonNull(protocolVersion, "protocolVersion");
    delegate.setProtocolVersion(protocolVersion);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setBrokers(String brokers) {
    requireNonNull(brokers, "brokers");
    delegate.setBrokers(brokers);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setTopic(String topic) {
    requireNonNull(topic, "topic");
    delegate.setTopic(topic);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setTimeout(long timeout, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(timeout >= 0, "timeout must be non-negative");
    delegate.setTimeout(timeout, unit);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setTimeout(Duration timeout) {
    requireNonNull(timeout, "timeout");
    return setTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setRetryPolicy(RetryPolicy retryPolicy) {
    requireNonNull(retryPolicy, "retryPolicy");
    delegate.setRetryPolicy(retryPolicy);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setMeterProvider(MeterProvider meterProvider) {
    requireNonNull(meterProvider, "meterProvider");
    setMeterProvider(() -> meterProvider);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaLogRecordExporterBuilder setMeterProvider(
      Supplier<MeterProvider> meterProviderSupplier) {
    requireNonNull(meterProviderSupplier, "meterProviderSupplier");
    delegate.setMeterProvider(meterProviderSupplier);
    return this;
  }

  public KafkaLogRecordExporter build() {
    return new KafkaLogRecordExporter(delegate, delegate.build());
  }
}
