/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.impl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import java.time.Duration;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * A builder for. {@link KafkaExporter}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class KafkaExporterBuilder<T extends Marshaler> {

  private static final Logger LOGGER = Logger.getLogger(KafkaExporterBuilder.class.getName());

  private final String exporterName;

  private final String type;

  private String protocolVersion = "2.0.0";

  private String brokers = "localhost:9092";

  /** otlp_spans for traces, otlp_metrics for metrics, otlp_logs for logs */
  private String topic = "topic";

  /** in milliseconds */
  private Long timeout = 1000L;

  @Nullable private RetryPolicy retryPolicy;

  private Supplier<MeterProvider> meterProviderSupplier = GlobalOpenTelemetry::getMeterProvider;

  public KafkaExporterBuilder(String exporterName, String type) {
    this.exporterName = exporterName;
    this.type = type;
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setBrokers(String brokers) {
    this.brokers = brokers;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setTopic(String topic) {
    this.topic = topic;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setTimeout(long timeout, TimeUnit unit) {
    this.timeout = unit.toMillis(timeout);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setTimeout(Duration timeout) {
    return setTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setRetryPolicy(RetryPolicy retryPolicy) {
    this.retryPolicy = retryPolicy;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaExporterBuilder<T> setMeterProvider(Supplier<MeterProvider> meterProviderSupplier) {
    this.meterProviderSupplier = meterProviderSupplier;
    return this;
  }

  @SuppressWarnings("BuilderReturnThis")
  public KafkaExporterBuilder<T> copy() {
    KafkaExporterBuilder<T> copy = new KafkaExporterBuilder<>(exporterName, type);

    copy.protocolVersion = protocolVersion;
    copy.brokers = brokers;
    copy.topic = topic;
    copy.timeout = timeout;
    if (retryPolicy != null) {
      copy.retryPolicy = retryPolicy.toBuilder().build();
    }
    copy.meterProviderSupplier = meterProviderSupplier;
    return copy;
  }

  public KafkaExporter<T> build() {
    KafkaSenderProvider kafkaSenderProvider = new KafkaSenderProvider();
    KafkaSender<T> kafkaSender = kafkaSenderProvider.createSender(brokers, topic, timeout);
    LOGGER.log(Level.FINE, "Using KafkaSender: " + kafkaSender.getClass().getName());
    return new KafkaExporter<>(exporterName, type, kafkaSender, meterProviderSupplier);
  }

  public String toString(boolean includePrefixAndSuffix) {
    StringJoiner joiner =
        includePrefixAndSuffix
            ? new StringJoiner(", ", "KafkaExporterBuilder{", "}")
            : new StringJoiner(", ");
    joiner.add("exporterName=" + exporterName);
    joiner.add("type=" + type);
    joiner.add("brokers=" + brokers);
    joiner.add("topic=" + topic);
    joiner.add("timeout=" + timeout);
    if (retryPolicy != null) {
      joiner.add("retryPolicy=" + retryPolicy);
    }
    // Note: omit tlsConfigHelper because we can't log the configuration in any readable way
    // Note: omit meterProviderSupplier because we can't log the configuration in any readable way
    return joiner.toString();
  }

  @Override
  public String toString() {
    return toString(true);
  }
}
