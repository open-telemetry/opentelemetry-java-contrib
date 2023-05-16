/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AwsSpanMetricsProcessor}. */
class AwsSpanMetricsProcessorTest {

  // Test constants
  private static final boolean CONTAINS_ATTRIBUTES = true;
  private static final boolean CONTAINS_NO_ATTRIBUTES = false;
  private static final double TEST_LATENCY_MILLIS = 150.0;
  private static final long TEST_LATENCY_NANOS = 150_000_000L;

  // Resource is not mockable, but tests can safely rely on an empty resource.
  private static final Resource testResource = Resource.empty();

  // Useful enum for indicating expected HTTP status code-related metrics
  private enum ExpectedStatusMetric {
    ERROR,
    FAULT,
    NEITHER
  }

  // Mocks required for tests.
  private LongCounter errorCounterMock;
  private LongCounter faultCounterMock;
  private DoubleHistogram latencyHistogramMock;
  private MetricAttributeGenerator generatorMock;

  private AwsSpanMetricsProcessor awsSpanMetricsProcessor;

  @BeforeEach
  public void setUpMocks() {
    errorCounterMock = mock(LongCounter.class);
    faultCounterMock = mock(LongCounter.class);
    latencyHistogramMock = mock(DoubleHistogram.class);
    generatorMock = mock(MetricAttributeGenerator.class);

    awsSpanMetricsProcessor =
        AwsSpanMetricsProcessor.create(
            errorCounterMock, faultCounterMock, latencyHistogramMock, generatorMock, testResource);
  }

  @Test
  public void testIsRequired() {
    assertThat(awsSpanMetricsProcessor.isStartRequired()).isFalse();
    assertThat(awsSpanMetricsProcessor.isEndRequired()).isTrue();
  }

  @Test
  public void testStartDoesNothingToSpan() {
    Context parentContextMock = mock(Context.class);
    ReadWriteSpan spanMock = mock(ReadWriteSpan.class);
    awsSpanMetricsProcessor.onStart(parentContextMock, spanMock);
    verifyNoInteractions(parentContextMock, spanMock);
  }

  @Test
  public void testTearDown() {
    assertThat(awsSpanMetricsProcessor.shutdown()).isEqualTo(CompletableResultCode.ofSuccess());
    assertThat(awsSpanMetricsProcessor.forceFlush()).isEqualTo(CompletableResultCode.ofSuccess());

    // Not really much to test, just check that it doesn't cause issues/throw anything.
    awsSpanMetricsProcessor.close();
  }

  /**
   * Tests starting with testOnEndMetricsGeneration are testing the logic in
   * AwsSpanMetricsProcessor's onEnd method pertaining to metrics generation.
   */
  @Test
  public void testOnEndMetricsGenerationWithoutSpanAttributes() {
    Attributes spanAttributes = buildSpanAttributes(CONTAINS_NO_ATTRIBUTES);
    ReadableSpan readableSpanMock = buildReadableSpanMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForOnEnd(readableSpanMock, metricAttributes);

    awsSpanMetricsProcessor.onEnd(readableSpanMock);
    verifyNoInteractions(errorCounterMock);
    verifyNoInteractions(faultCounterMock);
    verify(latencyHistogramMock, times(1)).record(eq(TEST_LATENCY_MILLIS), eq(metricAttributes));
  }

  @Test
  public void testOnEndMetricsGenerationWithoutMetricAttributes() {
    Attributes spanAttributes = Attributes.of(HTTP_STATUS_CODE, 500L);
    ReadableSpan readableSpanMock = buildReadableSpanMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_NO_ATTRIBUTES);
    configureMocksForOnEnd(readableSpanMock, metricAttributes);

    awsSpanMetricsProcessor.onEnd(readableSpanMock);
    verifyNoInteractions(errorCounterMock);
    verifyNoInteractions(faultCounterMock);
    verifyNoInteractions(latencyHistogramMock);
  }

  @Test
  public void testOnEndMetricsGenerationWithoutEndRequired() {
    Attributes spanAttributes = Attributes.of(HTTP_STATUS_CODE, 500L);
    ReadableSpan readableSpanMock = buildReadableSpanMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForOnEnd(readableSpanMock, metricAttributes);

    awsSpanMetricsProcessor.onEnd(readableSpanMock);
    verifyNoInteractions(errorCounterMock);
    verify(faultCounterMock, times(1)).add(eq(1L), eq(metricAttributes));
    verify(latencyHistogramMock, times(1)).record(eq(TEST_LATENCY_MILLIS), eq(metricAttributes));
  }

  @Test
  public void testOnEndMetricsGenerationWithLatency() {
    Attributes spanAttributes = Attributes.of(HTTP_STATUS_CODE, 200L);
    ReadableSpan readableSpanMock = buildReadableSpanMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForOnEnd(readableSpanMock, metricAttributes);

    when(readableSpanMock.getLatencyNanos()).thenReturn(5_500_000L);

    awsSpanMetricsProcessor.onEnd(readableSpanMock);
    verifyNoInteractions(errorCounterMock);
    verifyNoInteractions(faultCounterMock);
    verify(latencyHistogramMock, times(1)).record(eq(5.5), eq(metricAttributes));
  }

  @Test
  public void testOnEndMetricsGenerationWithStatusCodes() {
    // Invalid HTTP status codes
    validateMetricsGeneratedForHttpStatusCode(null, ExpectedStatusMetric.NEITHER);

    // Valid HTTP status codes
    validateMetricsGeneratedForHttpStatusCode(200L, ExpectedStatusMetric.NEITHER);
    validateMetricsGeneratedForHttpStatusCode(399L, ExpectedStatusMetric.NEITHER);
    validateMetricsGeneratedForHttpStatusCode(400L, ExpectedStatusMetric.ERROR);
    validateMetricsGeneratedForHttpStatusCode(499L, ExpectedStatusMetric.ERROR);
    validateMetricsGeneratedForHttpStatusCode(500L, ExpectedStatusMetric.FAULT);
    validateMetricsGeneratedForHttpStatusCode(599L, ExpectedStatusMetric.FAULT);
    validateMetricsGeneratedForHttpStatusCode(600L, ExpectedStatusMetric.NEITHER);
  }

  private static Attributes buildSpanAttributes(boolean containsAttribute) {
    if (containsAttribute) {
      return Attributes.of(AttributeKey.stringKey("original key"), "original value");
    } else {
      return Attributes.empty();
    }
  }

  private static Attributes buildMetricAttributes(boolean containsAttribute) {
    if (containsAttribute) {
      return Attributes.of(AttributeKey.stringKey("new key"), "new value");
    } else {
      return Attributes.empty();
    }
  }

  private static ReadableSpan buildReadableSpanMock(Attributes spanAttributes) {
    ReadableSpan readableSpanMock = mock(ReadableSpan.class);

    // Configure latency
    when(readableSpanMock.getLatencyNanos()).thenReturn(TEST_LATENCY_NANOS);

    // Configure attributes
    when(readableSpanMock.getAttribute(any()))
        .thenAnswer(invocation -> spanAttributes.get(invocation.getArgument(0)));

    // Configure spanData
    SpanData mockSpanData = mock(SpanData.class);
    when(mockSpanData.getAttributes()).thenReturn(spanAttributes);
    when(mockSpanData.getTotalAttributeCount()).thenReturn(spanAttributes.size());
    when(readableSpanMock.toSpanData()).thenReturn(mockSpanData);

    return readableSpanMock;
  }

  private void configureMocksForOnEnd(ReadableSpan readableSpanMock, Attributes metricAttributes) {
    // Configure generated attributes
    when(generatorMock.generateMetricAttributesFromSpan(
            eq(readableSpanMock.toSpanData()), eq(testResource)))
        .thenReturn(metricAttributes);
  }

  private void validateMetricsGeneratedForHttpStatusCode(
      Long httpStatusCode, ExpectedStatusMetric expectedStatusMetric) {
    Attributes spanAttributes = Attributes.of(HTTP_STATUS_CODE, httpStatusCode);
    ReadableSpan readableSpanMock = buildReadableSpanMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForOnEnd(readableSpanMock, metricAttributes);

    awsSpanMetricsProcessor.onEnd(readableSpanMock);
    switch (expectedStatusMetric) {
      case ERROR:
        verify(errorCounterMock, times(1)).add(eq(1L), eq(metricAttributes));
        verifyNoInteractions(faultCounterMock);
        break;
      case FAULT:
        verifyNoInteractions(errorCounterMock);
        verify(faultCounterMock, times(1)).add(eq(1L), eq(metricAttributes));
        break;
      case NEITHER:
        verifyNoInteractions(errorCounterMock);
        verifyNoInteractions(faultCounterMock);
        break;
    }

    verify(latencyHistogramMock, times(1)).record(eq(TEST_LATENCY_MILLIS), eq(metricAttributes));

    // Clear invocations so this method can be called multiple times in one test.
    clearInvocations(errorCounterMock);
    clearInvocations(faultCounterMock);
    clearInvocations(latencyHistogramMock);
  }
}
