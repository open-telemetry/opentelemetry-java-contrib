/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.annotation.concurrent.Immutable;

/**
 * This processor will generate metrics based on span data. It depends on a {@link
 * MetricAttributeGenerator} being provided on instantiation, which will provide a means to
 * determine attributes which should be used to create metrics. A {@link Resource} must also be
 * provided, which is used to generate metrics. Finally, two {@link LongCounter}'s and a {@link
 * DoubleHistogram} must be provided, which will be used to actually create desired metrics (see
 * below)
 *
 * <p>AwsSpanMetricsProcessor produces metrics for errors (e.g. HTTP 4XX status codes), faults (e.g.
 * HTTP 5XX status codes), and latency (in Milliseconds). Errors and faults are counted, while
 * latency is measured with a histogram. Metrics are emitted with attributes derived from span
 * attributes.
 *
 * <p>For highest fidelity metrics, this processor should be coupled with the {@link
 * AlwaysRecordSampler}, which will result in 100% of spans being sent to the processor.
 */
@Immutable
public final class AwsSpanMetricsProcessor implements SpanProcessor {

  private static final double NANOS_TO_MILLIS = 1_000_000.0;

  // Constants for deriving error and fault metrics
  private static final int ERROR_CODE_LOWER_BOUND = 400;
  private static final int ERROR_CODE_UPPER_BOUND = 499;
  private static final int FAULT_CODE_LOWER_BOUND = 500;
  private static final int FAULT_CODE_UPPER_BOUND = 599;

  // Metric instruments
  private final LongCounter errorCounter;
  private final LongCounter faultCounter;
  private final DoubleHistogram latencyHistogram;

  private final MetricAttributeGenerator generator;
  private final Resource resource;

  /** Use {@link AwsSpanMetricsProcessorBuilder} to construct this processor. */
  static AwsSpanMetricsProcessor create(
      LongCounter errorCounter,
      LongCounter faultCounter,
      DoubleHistogram latencyHistogram,
      MetricAttributeGenerator generator,
      Resource resource) {
    return new AwsSpanMetricsProcessor(
        errorCounter, faultCounter, latencyHistogram, generator, resource);
  }

  private AwsSpanMetricsProcessor(
      LongCounter errorCounter,
      LongCounter faultCounter,
      DoubleHistogram latencyHistogram,
      MetricAttributeGenerator generator,
      Resource resource) {
    this.errorCounter = errorCounter;
    this.faultCounter = faultCounter;
    this.latencyHistogram = latencyHistogram;
    this.generator = generator;
    this.resource = resource;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    SpanData spanData = span.toSpanData();
    Attributes attributes = generator.generateMetricAttributesFromSpan(spanData, resource);

    // Only record metrics if non-empty attributes are returned.
    if (!attributes.isEmpty()) {
      recordErrorOrFault(span, attributes);
      recordLatency(span, attributes);
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  private void recordErrorOrFault(ReadableSpan span, Attributes attributes) {
    Long httpStatusCode = span.getAttribute(HTTP_STATUS_CODE);
    if (httpStatusCode == null) {
      return;
    }

    if (httpStatusCode >= ERROR_CODE_LOWER_BOUND && httpStatusCode <= ERROR_CODE_UPPER_BOUND) {
      errorCounter.add(1, attributes);
    } else if (httpStatusCode >= FAULT_CODE_LOWER_BOUND
        && httpStatusCode <= FAULT_CODE_UPPER_BOUND) {
      faultCounter.add(1, attributes);
    }
  }

  private void recordLatency(ReadableSpan span, Attributes attributes) {
    long nanos = span.getLatencyNanos();
    double millis = nanos / NANOS_TO_MILLIS;
    latencyHistogram.record(millis, attributes);
  }
}
