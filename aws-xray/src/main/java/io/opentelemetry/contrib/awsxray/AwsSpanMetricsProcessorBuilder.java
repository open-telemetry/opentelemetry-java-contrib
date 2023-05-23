/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.resources.Resource;

/** A builder for {@link AwsSpanMetricsProcessor} */
public final class AwsSpanMetricsProcessorBuilder {

  // Metric instrument configuration constants
  private static final String ERROR = "Error";
  private static final String FAULT = "Fault";
  private static final String LATENCY = "Latency";
  private static final String LATENCY_UNITS = "Milliseconds";

  // Defaults
  private static final MetricAttributeGenerator DEFAULT_GENERATOR =
      new AwsMetricAttributeGenerator();
  private static final String DEFAULT_SCOPE_NAME = "AwsSpanMetricsProcessor";

  // Required builder elements
  private final MeterProvider meterProvider;
  private final Resource resource;

  // Optional builder elements
  private MetricAttributeGenerator generator = DEFAULT_GENERATOR;
  private String scopeName = DEFAULT_SCOPE_NAME;

  public static AwsSpanMetricsProcessorBuilder create(
      MeterProvider meterProvider, Resource resource) {
    return new AwsSpanMetricsProcessorBuilder(meterProvider, resource);
  }

  private AwsSpanMetricsProcessorBuilder(MeterProvider meterProvider, Resource resource) {
    this.meterProvider = meterProvider;
    this.resource = resource;
  }

  /**
   * Sets the generator used to generate attributes used in metrics produced by span metrics
   * processor. If unset, defaults to {@link #DEFAULT_GENERATOR}. Must not be null.
   */
  @CanIgnoreReturnValue
  public AwsSpanMetricsProcessorBuilder setGenerator(MetricAttributeGenerator generator) {
    requireNonNull(generator, "generator");
    this.generator = generator;
    return this;
  }

  /**
   * Sets the scope name used in the creation of metrics by the span metrics processor. If unset,
   * defaults to {@link #DEFAULT_SCOPE_NAME}. Must not be null.
   */
  @CanIgnoreReturnValue
  public AwsSpanMetricsProcessorBuilder setScopeName(String scopeName) {
    requireNonNull(scopeName, "scopeName");
    this.scopeName = scopeName;
    return this;
  }

  public AwsSpanMetricsProcessor build() {
    Meter meter = meterProvider.get(scopeName);
    LongCounter errorCounter = meter.counterBuilder(ERROR).build();
    LongCounter faultCounter = meter.counterBuilder(FAULT).build();
    DoubleHistogram latencyHistogram =
        meter.histogramBuilder(LATENCY).setUnit(LATENCY_UNITS).build();

    return AwsSpanMetricsProcessor.create(
        errorCounter, faultCounter, latencyHistogram, generator, resource);
  }
}
