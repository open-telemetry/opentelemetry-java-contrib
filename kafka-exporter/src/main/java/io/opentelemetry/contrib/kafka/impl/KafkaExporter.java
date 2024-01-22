/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.impl;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterMetrics;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic Kafka exporter.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@SuppressWarnings("checkstyle:JavadocMethod")
public final class KafkaExporter<T extends Marshaler> {

  private static final Logger internalLogger = Logger.getLogger(KafkaExporter.class.getName());

  private final ThrottlingLogger logger = new ThrottlingLogger(internalLogger);

  private final AtomicBoolean isShutdown = new AtomicBoolean();

  private final String type;

  private final KafkaSender<T> kafkaSender;

  private final ExporterMetrics exporterMetrics;

  public KafkaExporter(
      String exporterName,
      String type,
      KafkaSender<T> kafkaSender,
      Supplier<MeterProvider> meterProviderSupplier) {
    this.type = type;
    this.kafkaSender = kafkaSender;
    this.exporterMetrics = ExporterMetrics.createGrpc(exporterName, type, meterProviderSupplier);
  }

  public CompletableResultCode export(T exportRequest, int numItems) {
    if (isShutdown.get()) {
      return CompletableResultCode.ofFailure();
    }

    exporterMetrics.addSeen(numItems);

    CompletableResultCode result = new CompletableResultCode();

    kafkaSender.send(
        exportRequest,
        () -> {
          exporterMetrics.addSuccess(numItems);
          result.succeed();
        },
        (response, throwable) -> {
          exporterMetrics.addFailed(numItems);
          if (logger.isLoggable(Level.FINEST)) {
            logger.log(
                Level.FINEST, "Failed to export " + type + "s. Details follow: " + throwable);
          }
          result.fail();
        });

    return result;
  }

  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      logger.log(Level.INFO, "Calling shutdown() multiple times.");
      return CompletableResultCode.ofSuccess();
    }
    return kafkaSender.shutdown();
  }
}
