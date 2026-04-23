/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ConsistentReservoirSamplingSpanProcessorTest {

  private static final int RESERVOIR_SIZE = 10;
  private static final long EXPORT_PERIOD_100_MILLIS_AS_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

  @Test
  void invalidConfig() {
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    assertThatNullPointerException()
        .isThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(null, 1, 1))
        .withMessage("spanExporter");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(exporter, -1, 1))
        .withMessage("reservoir size must be positive");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(exporter, 1, -1))
        .withMessage("export period must be positive");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(exporter, 1, 1, -1))
        .withMessage("exporter timeout must be positive");
    exporter.shutdown();
  }

  @Test
  void startEndRequirements() {
    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            InMemorySpanExporter.create(), RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS);
    assertThat(processor.isStartRequired()).isFalse();
    assertThat(processor.isEndRequired()).isTrue();
    processor.shutdown().join(1, TimeUnit.SECONDS);
  }

  @Test
  @Timeout(10)
  void exportsSpans() {
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            exporter, RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS);
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(processor)
            .setSampler(CompositeSampler.wrap(ComposableSampler.alwaysOn()))
            .build();

    int numberOfSpans = 3;
    for (int i = 0; i < numberOfSpans; i++) {
      sdkTracerProvider.get("test").spanBuilder("span-" + i).startSpan().end();
    }

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(exporter.getFinishedSpanItems()).hasSize(numberOfSpans));

    sdkTracerProvider.close();
  }

  @Test
  @Timeout(10)
  void reservoirCaps() {
    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            exporter, RESERVOIR_SIZE, TimeUnit.SECONDS.toNanos(60));
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(processor)
            .setSampler(CompositeSampler.wrap(ComposableSampler.alwaysOn()))
            .build();

    int numberOfSpans = 1000;
    for (int i = 0; i < numberOfSpans; i++) {
      sdkTracerProvider.get("test").spanBuilder("span-" + i).startSpan().end();
    }

    processor.forceFlush().join(5, TimeUnit.SECONDS);

    // At most `RESERVOIR_SIZE` spans should have been exported for the current period.
    assertThat(exporter.getFinishedSpanItems().size()).isLessThanOrEqualTo(RESERVOIR_SIZE);

    sdkTracerProvider.close();
  }
}
