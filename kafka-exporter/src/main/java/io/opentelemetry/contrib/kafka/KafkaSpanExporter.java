/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@SuppressWarnings("FutureReturnValueIgnored")
public class KafkaSpanExporter implements SpanExporter {
  private static final Logger logger = LoggerFactory.getLogger(KafkaSpanExporter.class);
  private final String topicName;
  private final Producer<String, Collection<SpanData>> producer;
  private final ExecutorService executorService;
  private final long timeoutInSeconds;
  private final AtomicBoolean isShutdown = new AtomicBoolean();

  public static KafkaSpanExporterBuilder newBuilder() {
    return new KafkaSpanExporterBuilder();
  }

  KafkaSpanExporter(
      String topicName,
      Producer<String, Collection<SpanData>> producer,
      ExecutorService executorService,
      long timeoutInSeconds) {
    this.topicName = topicName;
    this.producer = producer;
    this.executorService = executorService;
    this.timeoutInSeconds = timeoutInSeconds;
  }

  @Override
  public CompletableResultCode export(@Nonnull Collection<SpanData> spans) {
    if (isShutdown.get()) {
      return CompletableResultCode.ofFailure();
    }
    ProducerRecord<String, Collection<SpanData>> producerRecord =
        new ProducerRecord<>(topicName, spans);

    CompletableResultCode result = new CompletableResultCode();
    CompletableFuture.runAsync(
        () ->
            producer.send(
                producerRecord,
                (metadata, exception) -> {
                  if (exception == null) {
                    result.succeed();
                  } else {
                    logger.error(
                        String.format("Error while sending spans to Kafka topic %s", topicName),
                        exception);
                    result.fail();
                  }
                }),
        executorService).whenComplete((ignore, exception) -> {
          if (exception != null) {
            logger.error("Executor task failed while sending to Kafka topic {}", topicName, exception);
            result.fail();
          }
        });
    return result;
  }

  @Override
  public CompletableResultCode flush() {
    CompletableResultCode result = new CompletableResultCode();
    CompletableFuture.runAsync(producer::flush, executorService)
        .handle(
            (unused, exception) -> {
              if (exception == null) {
                result.succeed();
              } else {
                logger.error(
                    String.format(
                        "Error while performing the flush operation on topic %s", topicName),
                    exception);
                result.fail();
              }
              return true;
            });
    return result;
  }

  private CompletableResultCode shutdownExecutorService() {
    try {
      executorService.shutdown();
      boolean terminated = executorService.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS);
      if (!terminated) {
        List<Runnable> interrupted = executorService.shutdownNow();
        if (!interrupted.isEmpty()) {
          logger.error(
              "Shutting down KafkaSpanExporter forced {} tasks to be cancelled.",
              interrupted.size());
        }
      }
      return CompletableResultCode.ofSuccess();
    } catch (InterruptedException e) {
      logger.error("Error when trying to shutdown KafkaSpanExporter executorService.", e);
      return CompletableResultCode.ofFailure();
    }
  }

  private CompletableResultCode shutdownProducer() {
    try {
      producer.close(Duration.ofSeconds(timeoutInSeconds));
      return CompletableResultCode.ofSuccess();
    } catch (KafkaException e) {
      logger.error("Error when trying to shutdown KafkaSpanExporter Producer.", e);
      return CompletableResultCode.ofFailure();
    }
  }

  @Override
  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      logger.warn("Calling shutdown() multiple times.");
      return CompletableResultCode.ofSuccess();
    }
    List<CompletableResultCode> codes = new ArrayList<>(2);
    codes.add(shutdownExecutorService());
    codes.add(shutdownProducer());
    return CompletableResultCode.ofAll(codes);
  }
}
