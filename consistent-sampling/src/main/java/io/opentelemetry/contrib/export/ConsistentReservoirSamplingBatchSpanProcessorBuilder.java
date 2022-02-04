/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.export;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.util.RandomGenerator;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** Builder class for {@link ConsistentReservoirSamplingBatchSpanProcessorBuilder}. */
public final class ConsistentReservoirSamplingBatchSpanProcessorBuilder {

  // Visible for testing
  static final long DEFAULT_SCHEDULE_DELAY_MILLIS = 5000;
  // Visible for testing
  static final int DEFAULT_RESERVOIR_SIZE = 2048;
  // Visible for testing
  static final int DEFAULT_EXPORT_TIMEOUT_MILLIS = 30_000;

  private final SpanExporter spanExporter;
  private long scheduleDelayNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_SCHEDULE_DELAY_MILLIS);
  private int reservoirSize = DEFAULT_RESERVOIR_SIZE;
  private long exporterTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_EXPORT_TIMEOUT_MILLIS);
  private MeterProvider meterProvider = MeterProvider.noop();
  private RandomGenerator threadSafeRandomGenerator = () -> ThreadLocalRandom.current().nextLong();
  private boolean useAlternativeReservoirImplementation = false;

  ConsistentReservoirSamplingBatchSpanProcessorBuilder(SpanExporter spanExporter) {
    this.spanExporter = requireNonNull(spanExporter, "spanExporter");
  }

  // TODO: Consider to add support for constant Attributes and/or Resource.

  /**
   * Sets the delay interval between two consecutive exports. If unset, defaults to {@value
   * DEFAULT_SCHEDULE_DELAY_MILLIS}ms.
   */
  public ConsistentReservoirSamplingBatchSpanProcessorBuilder setScheduleDelay(
      long delay, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(delay >= 0, "delay must be non-negative");
    scheduleDelayNanos = unit.toNanos(delay);
    return this;
  }

  /**
   * Sets the delay interval between two consecutive exports. If unset, defaults to {@value
   * DEFAULT_SCHEDULE_DELAY_MILLIS}ms.
   */
  public ConsistentReservoirSamplingBatchSpanProcessorBuilder setScheduleDelay(Duration delay) {
    requireNonNull(delay, "delay");
    return setScheduleDelay(delay.toNanos(), TimeUnit.NANOSECONDS);
  }

  // Visible for testing
  long getScheduleDelayNanos() {
    return scheduleDelayNanos;
  }

  /**
   * Sets the maximum time an export will be allowed to run before being cancelled. If unset,
   * defaults to {@value DEFAULT_EXPORT_TIMEOUT_MILLIS}ms.
   */
  public ConsistentReservoirSamplingBatchSpanProcessorBuilder setExporterTimeout(
      long timeout, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(timeout >= 0, "timeout must be non-negative");
    exporterTimeoutNanos = unit.toNanos(timeout);
    return this;
  }

  /**
   * Sets the maximum time an export will be allowed to run before being cancelled. If unset,
   * defaults to {@value DEFAULT_EXPORT_TIMEOUT_MILLIS}ms.
   */
  public ConsistentReservoirSamplingBatchSpanProcessorBuilder setExporterTimeout(Duration timeout) {
    requireNonNull(timeout, "timeout");
    return setExporterTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
  }

  // Visible for testing
  long getExporterTimeoutNanos() {
    return exporterTimeoutNanos;
  }

  /**
   * Sets the reservoir size, themaximum number of Spans that can be collected.
   *
   * <p>See the ConsistentReservoirSamplingBatchSpanProcessor class description for a high-level
   * design description of this class.
   *
   * <p>Default value is {@code 2048}.
   *
   * @param reservoirSize the reservoir size, the maximum number of Spans that are kept
   * @return this.
   * @see ConsistentReservoirSamplingBatchSpanProcessorBuilder#DEFAULT_RESERVOIR_SIZE
   */
  public ConsistentReservoirSamplingBatchSpanProcessorBuilder setReservoirSize(int reservoirSize) {
    this.reservoirSize = reservoirSize;
    return this;
  }

  // Visible for testing
  int getReservoirSize() {
    return reservoirSize;
  }

  /**
   * Sets the {@link MeterProvider} to use to collect metrics related to batch export. If not set,
   * metrics will not be collected.
   */
  public ConsistentReservoirSamplingBatchSpanProcessorBuilder setMeterProvider(
      MeterProvider meterProvider) {
    requireNonNull(meterProvider, "meterProvider");
    this.meterProvider = meterProvider;
    return this;
  }

  // Visible for testing
  ConsistentReservoirSamplingBatchSpanProcessorBuilder setThreadSafeRandomGenerator(
      RandomGenerator threadSafeRandomGenerator) {
    this.threadSafeRandomGenerator = threadSafeRandomGenerator;
    return this;
  }

  // Visible for testing
  ConsistentReservoirSamplingBatchSpanProcessorBuilder useAlternativeReservoirImplementation(
      boolean useAlternativeReservoirImplementation) {
    this.useAlternativeReservoirImplementation = useAlternativeReservoirImplementation;
    return this;
  }

  /**
   * Returns a new {@link ConsistentReservoirSamplingBatchSpanProcessorBuilder} that batches, then
   * converts spans to proto and forwards them to the given {@code spanExporter}.
   *
   * @return a new {@link ConsistentReservoirSamplingBatchSpanProcessorBuilder}.
   * @throws NullPointerException if the {@code spanExporter} is {@code null}.
   */
  public ConsistentReservoirSamplingBatchSpanProcessor build() {
    return new ConsistentReservoirSamplingBatchSpanProcessor(
        spanExporter,
        meterProvider,
        scheduleDelayNanos,
        reservoirSize,
        exporterTimeoutNanos,
        threadSafeRandomGenerator,
        useAlternativeReservoirImplementation);
  }
}
