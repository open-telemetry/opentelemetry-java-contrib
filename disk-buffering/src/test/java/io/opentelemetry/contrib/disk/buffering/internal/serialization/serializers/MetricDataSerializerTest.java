/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import static java.util.Collections.singletonList;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoubleExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableValueAtQuantile;
import java.util.Arrays;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class MetricDataSerializerTest extends BaseSignalSerializerTest<MetricData> {

  @Test
  void verifyLongGauge() {
    MetricData metric = createLongGauge(TraceFlags.getDefault());
    MetricData expected = createLongGauge(TraceFlags.getSampled());
    assertSerializeDeserialize(metric, expected);
  }

  @Test
  void verifyDoubleGauge() {
    MetricData metric = createDoubleGauge(TraceFlags.getDefault());
    // This silly work exists here so that we can verify that the exemplar that we get back has a
    // span context
    // whose flags are set to something different. This is because the trace flags are NOT part of
    // the exemplar
    // protos, and what we get back is not what we put in.
    MetricData expected = createDoubleGauge(TraceFlags.getSampled());
    assertSerializeDeserialize(metric, expected);
  }

  @Test
  void verifyLongSum() {
    MetricData metric = makeLongSum(TraceFlags.getDefault());
    MetricData expected = makeLongSum(TraceFlags.getSampled());
    assertSerializeDeserialize(metric, expected);
  }

  @Test
  void verifySummary() {
    ValueAtQuantile value = ImmutableValueAtQuantile.create(2.0, 1.0);

    SummaryPointData pointData =
        ImmutableSummaryPointData.create(
            1L, 2L, TestData.ATTRIBUTES, 1L, 2.0, singletonList(value));

    SummaryData data = ImmutableSummaryData.create(singletonList(pointData));
    MetricData metric =
        ImmutableMetricData.createDoubleSummary(
            TestData.RESOURCE_FULL,
            TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
            "Summary name",
            "Summary description",
            "ms",
            data);
    assertSerialization(metric);
  }

  @Test
  void verifyDoubleSum() {
    MetricData metric = makeDoubleSum(TraceFlags.getDefault());
    MetricData expected = makeDoubleSum(TraceFlags.getSampled());
    assertSerializeDeserialize(metric, expected);
  }

  @Test
  void verifyHistogram() {
    MetricData metric = makeHistogram(TraceFlags.getDefault());
    MetricData expected = makeHistogram(TraceFlags.getSampled());
    assertSerializeDeserialize(metric, expected);
  }

  @Test
  void verifyExponentialHistogram() {
    MetricData metric = makeExponentialHistogram(TraceFlags.getDefault());
    MetricData expected = makeExponentialHistogram(TraceFlags.getSampled());
    assertSerializeDeserialize(metric, expected);
  }

  @NotNull
  private static MetricData createLongGauge(TraceFlags flags) {
    LongPointData pointData = makeLongPointData(flags);
    GaugeData<LongPointData> data = ImmutableGaugeData.create(singletonList(pointData));
    return ImmutableMetricData.createLongGauge(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Long gauge name",
        "Long gauge description",
        "ms",
        data);
  }

  @NotNull
  private static DoublePointData makeDoublePointData(TraceFlags flags) {
    DoubleExemplarData doubleExemplarData = makeDoubleExemplar(flags);
    return ImmutableDoublePointData.create(
        1L, 2L, TestData.ATTRIBUTES, 1.0, singletonList(doubleExemplarData));
  }

  @NotNull
  private static MetricData createDoubleGauge(TraceFlags flags) {
    DoublePointData point = makeDoublePointData(flags);
    GaugeData<DoublePointData> gaugeData = ImmutableGaugeData.create(singletonList(point));
    MetricData metric =
        ImmutableMetricData.createDoubleGauge(
            TestData.RESOURCE_FULL,
            TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
            "Double gauge name",
            "Double gauge description",
            "ms",
            gaugeData);
    return metric;
  }

  @NotNull
  private static DoubleExemplarData makeDoubleExemplar(TraceFlags flags) {
    SpanContext expectedExemplarContext = createSpanContext(flags);
    return ImmutableDoubleExemplarData.create(
        TestData.ATTRIBUTES, 100L, expectedExemplarContext, 1.0);
  }

  @NotNull
  private static MetricData makeLongSum(TraceFlags flags) {
    LongPointData point = makeLongPointData(flags);
    SumData<LongPointData> sumData =
        ImmutableSumData.create(true, AggregationTemporality.DELTA, singletonList(point));

    return ImmutableMetricData.createLongSum(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Long sum name",
        "Long sum description",
        "ms",
        sumData);
  }

  @NotNull
  private static LongPointData makeLongPointData(TraceFlags flags) {
    LongExemplarData ex = makeLongExemplar(flags);
    return ImmutableLongPointData.create(1L, 2L, TestData.ATTRIBUTES, 1L, singletonList(ex));
  }

  @NotNull
  private static LongExemplarData makeLongExemplar(TraceFlags flags) {
    SpanContext context = createSpanContext(flags);
    return ImmutableLongExemplarData.create(TestData.ATTRIBUTES, 100L, context, 1L);
  }

  @NotNull
  private static SpanContext createSpanContext(TraceFlags flags) {
    return SpanContext.create(TestData.TRACE_ID, TestData.SPAN_ID, flags, TraceState.getDefault());
  }

  @NotNull
  private static MetricData makeDoubleSum(TraceFlags flags) {
    DoublePointData point = makeDoublePointData(flags);
    SumData<DoublePointData> DOUBLE_SUM_DATA =
        ImmutableSumData.create(true, AggregationTemporality.DELTA, singletonList(point));
    return ImmutableMetricData.createDoubleSum(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Double sum name",
        "Double sum description",
        "ms",
        DOUBLE_SUM_DATA);
  }

  @NotNull
  private static MetricData makeExponentialHistogram(TraceFlags flags) {
    ExponentialHistogramBuckets positiveBucket =
        ImmutableExponentialHistogramBuckets.create(1, 10, Arrays.asList(1L, 10L));

    ExponentialHistogramBuckets negativeBucket =
        ImmutableExponentialHistogramBuckets.create(1, 0, Collections.emptyList());

    DoubleExemplarData exemplar = makeDoubleExemplar(flags);
    ExponentialHistogramPointData pointData =
        ImmutableExponentialHistogramPointData.create(
            1,
            10.0,
            1L,
            true,
            2.0,
            true,
            4.0,
            positiveBucket,
            negativeBucket,
            1L,
            2L,
            TestData.ATTRIBUTES,
            singletonList(exemplar));
    ExponentialHistogramData data =
        ImmutableExponentialHistogramData.create(
            AggregationTemporality.CUMULATIVE, singletonList(pointData));
    return ImmutableMetricData.createExponentialHistogram(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Exponential histogram name",
        "Exponential histogram description",
        "ms",
        data);
  }

  @NotNull
  private static MetricData makeHistogram(TraceFlags flags) {
    DoubleExemplarData exemplar = makeDoubleExemplar(flags);
    HistogramPointData pointData =
        ImmutableHistogramPointData.create(
            1L,
            2L,
            TestData.ATTRIBUTES,
            15.0,
            true,
            4.0,
            true,
            7.0,
            singletonList(10.0),
            Arrays.asList(1L, 2L),
            singletonList(exemplar));

    HistogramData data =
        ImmutableHistogramData.create(AggregationTemporality.CUMULATIVE, singletonList(pointData));

    return ImmutableMetricData.createDoubleHistogram(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Histogram name",
        "Histogram description",
        "ms",
        data);
  }

  @Override
  protected SignalSerializer<MetricData> getSerializer() {
    return SignalSerializer.ofMetrics();
  }

  @Override
  protected SignalDeserializer<MetricData> getDeserializer() {
    return SignalDeserializer.ofMetrics();
  }
}
