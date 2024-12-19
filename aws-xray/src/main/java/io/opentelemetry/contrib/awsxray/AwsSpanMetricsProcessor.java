/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
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

  private static final AttributeKey<Long> HTTP_STATUS_CODE =
      AttributeKey.longKey("http.status_code");

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
      recordErrorOrFault(spanData, attributes);
      recordLatency(span, attributes);
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  private void recordErrorOrFault(SpanData spanData, Attributes attributes) {
    Long httpStatusCode = spanData.getAttributes().get(HTTP_STATUS_CODE);
    if (httpStatusCode == null) {
      httpStatusCode = getAwsStatusCode(spanData);

      if (httpStatusCode == null || httpStatusCode < 100L || httpStatusCode > 599L) {
        return;
      }
    }

    if (httpStatusCode >= ERROR_CODE_LOWER_BOUND && httpStatusCode <= ERROR_CODE_UPPER_BOUND) {
      errorCounter.add(1, attributes);
    } else if (httpStatusCode >= FAULT_CODE_LOWER_BOUND
        && httpStatusCode <= FAULT_CODE_UPPER_BOUND) {
      faultCounter.add(1, attributes);
    }
  }

  /**
   * Attempt to pull status code from spans produced by AWS SDK instrumentation (both v1 and v2).
   * AWS SDK instrumentation does not populate http.status_code when non-200 status codes are
   * returned, as the AWS SDK throws exceptions rather than returning responses with status codes.
   * To work around this, we are attempting to get the exception out of the events, then calling
   * getStatusCode (for AWS SDK V1) and statusCode (for AWS SDK V2) to get the status code fromt the
   * exception. We rely on reflection here because we cannot cast the throwable to
   * AmazonServiceExceptions (V1) or AwsServiceExceptions (V2) because the throwable comes from a
   * separate class loader and attempts to cast will fail with ClassCastException.
   *
   * <p>TODO: Short term workaround. This can be completely removed once
   * https://github.com/open-telemetry/opentelemetry-java-contrib/issues/919 is resolved.
   */
  @Nullable
  private static Long getAwsStatusCode(SpanData spanData) {
    String scopeName = spanData.getInstrumentationScopeInfo().getName();
    if (!scopeName.contains("aws-sdk")) {
      return null;
    }

    for (EventData event : spanData.getEvents()) {
      if (event instanceof ExceptionEventData) {
        ExceptionEventData exceptionEvent = (ExceptionEventData) event;
        Throwable throwable = exceptionEvent.getException();

        try {
          Method method = throwable.getClass().getMethod("getStatusCode", new Class<?>[] {});
          Object code = method.invoke(throwable, new Object[] {});
          return Long.valueOf((Integer) code);
        } catch (Exception e) {
          // Take no action
        }

        try {
          Method method = throwable.getClass().getMethod("statusCode", new Class<?>[] {});
          Object code = method.invoke(throwable, new Object[] {});
          return Long.valueOf((Integer) code);
        } catch (Exception e) {
          // Take no action
        }
      }
    }

    return null;
  }

  private void recordLatency(ReadableSpan span, Attributes attributes) {
    long nanos = span.getLatencyNanos();
    double millis = nanos / NANOS_TO_MILLIS;
    latencyHistogram.record(millis, attributes);
  }
}
