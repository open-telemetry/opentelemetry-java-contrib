/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import io.opentelemetry.contrib.kafka.impl.KafkaExporter;
import io.opentelemetry.contrib.kafka.impl.KafkaExporterBuilder;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link LogRecordExporter} which writes {@linkplain LogRecordData logs} to Kafka in OLTP format.
 */
public final class KafkaLogRecordExporter implements LogRecordExporter {

  private static final Logger logger = Logger.getLogger(KafkaLogRecordExporter.class.getName());

  private final AtomicBoolean isShutdown = new AtomicBoolean();

  private final KafkaExporterBuilder<LogsRequestMarshaler> builder;

  private final KafkaExporter<LogsRequestMarshaler> delegate;

  /**
   * Returns a new {@link KafkaLogRecordExporter} using the default values.
   *
   * <p>To load configuration values from environment variables and system properties, use <a
   * href="https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure">opentelemetry-sdk-extension-autoconfigure</a>.
   *
   * @return a new {@link KafkaLogRecordExporter} instance.
   */
  public static KafkaLogRecordExporter getDefault() {
    return builder().build();
  }

  /**
   * Returns a new builder instance for this exporter.
   *
   * @return a new builder instance for this exporter.
   */
  public static KafkaLogRecordExporterBuilder builder() {
    return new KafkaLogRecordExporterBuilder();
  }

  KafkaLogRecordExporter(
      KafkaExporterBuilder<LogsRequestMarshaler> builder,
      KafkaExporter<LogsRequestMarshaler> delegate) {
    this.builder = builder;
    this.delegate = delegate;
  }

  public KafkaLogRecordExporterBuilder toBuilder() {
    return new KafkaLogRecordExporterBuilder(builder.copy());
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    if (isShutdown.get()) {
      return CompletableResultCode.ofFailure();
    }
    try {
      LogsRequestMarshaler exportRequest = LogsRequestMarshaler.create(logs);
      return delegate.export(exportRequest, logs.size());
    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "send log records to kafka failed.", e);
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
    return "KafkaLogRecordExporter{" + builder.toString(false) + "}";
  }
}
