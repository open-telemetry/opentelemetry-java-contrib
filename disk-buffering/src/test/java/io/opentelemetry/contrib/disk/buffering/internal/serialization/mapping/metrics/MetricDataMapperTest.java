/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
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
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class MetricDataMapperTest {

  @Test
  void verifyLongGaugeMapping() {
    MetricData longGauge = makeLongGauge(TraceFlags.getDefault());
    MetricData expected = makeLongGauge(TraceFlags.getSampled());

    Metric proto = mapToProto(longGauge);

    MetricData result =
        mapToSdk(proto, longGauge.getResource(), longGauge.getInstrumentationScopeInfo());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void verifyDoubleGaugeMapping() {
    MetricData doubleGauge = makeDoubleGauge(TraceFlags.getDefault());
    MetricData expected = makeDoubleGauge(TraceFlags.getSampled());

    Metric proto = mapToProto(doubleGauge);
    MetricData result =
        mapToSdk(proto, doubleGauge.getResource(), doubleGauge.getInstrumentationScopeInfo());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void verifyLongSumMapping() {
    MetricData longSum = makeLongSum(TraceFlags.getDefault());
    MetricData expected = makeLongSum(TraceFlags.getSampled());

    Metric proto = mapToProto(longSum);
    MetricData result =
        mapToSdk(proto, TestData.RESOURCE_FULL, longSum.getInstrumentationScopeInfo());
    assertThat(expected).isEqualTo(result);
  }

  @Test
  void verifyDoubleSumMapping() {
    MetricData doubleSum = makeDoubleSum(TraceFlags.getDefault());
    MetricData expected = makeDoubleSum(TraceFlags.getSampled());

    Metric proto = mapToProto(doubleSum);

    MetricData result =
        mapToSdk(proto, doubleSum.getResource(), doubleSum.getInstrumentationScopeInfo());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void verifySummaryMapping() {
    ValueAtQuantile value = ImmutableValueAtQuantile.create(2.0, 1.0);
    SummaryPointData pointData =
        ImmutableSummaryPointData.create(
            1L, 2L, TestData.ATTRIBUTES, 1L, 2.0, Collections.singletonList(value));

    SummaryData data = ImmutableSummaryData.create(Collections.singletonList(pointData));

    MetricData summaryMetric =
        ImmutableMetricData.createDoubleSummary(
            TestData.RESOURCE_FULL,
            TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
            "Summary name",
            "Summary description",
            "ms",
            data);

    Metric proto = mapToProto(summaryMetric);

    assertEquals(
        summaryMetric,
        mapToSdk(proto, summaryMetric.getResource(), summaryMetric.getInstrumentationScopeInfo()));
  }

  @Test
  void verifyHistogramMapping() {

    MetricData histogramMetric = makeHistogram(TraceFlags.getDefault());
    MetricData expected = makeHistogram(TraceFlags.getSampled());

    Metric proto = mapToProto(histogramMetric);

    MetricData result =
        mapToSdk(
            proto, histogramMetric.getResource(), histogramMetric.getInstrumentationScopeInfo());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void verifyExponentialHistogramMapping() {
    MetricData histogram = makeExponentialHistogram(TraceFlags.getDefault());
    MetricData expected = makeExponentialHistogram(TraceFlags.getSampled());

    Metric proto = mapToProto(histogram);

    MetricData result =
        mapToSdk(proto, histogram.getResource(), histogram.getInstrumentationScopeInfo());

    assertThat(result).isEqualTo(expected);
  }

  @NotNull
  private static MetricData makeDoubleGauge(TraceFlags flags) {
    DoublePointData point = makeDoublePointData(flags);
    return ImmutableMetricData.createDoubleGauge(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Double gauge name",
        "Double gauge description",
        "ms",
        ImmutableGaugeData.create(Collections.singletonList(point)));
  }

  @NotNull
  private static List<DoubleExemplarData> makeDoubleExemplars(TraceFlags flags) {
    SpanContext context = makeContext(flags);
    DoubleExemplarData doubleExemplarData =
        ImmutableDoubleExemplarData.create(TestData.ATTRIBUTES, 100L, context, 1.0);
    return Collections.singletonList(doubleExemplarData);
  }

  @NotNull
  private static MetricData makeLongGauge(TraceFlags flags) {
    LongPointData point = makeLongPointData(flags);
    GaugeData<LongPointData> gaugeData =
        ImmutableGaugeData.create(Collections.singletonList(point));
    return ImmutableMetricData.createLongGauge(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Long gauge name",
        "Long gauge description",
        "ms",
        gaugeData);
  }

  @NotNull
  private static LongExemplarData makeLongExemplarData(TraceFlags flags) {
    SpanContext context = makeContext(flags);
    return ImmutableLongExemplarData.create(TestData.ATTRIBUTES, 100L, context, 1L);
  }

  @NotNull
  private static MetricData makeLongSum(TraceFlags flags) {
    LongPointData pointData = makeLongPointData(flags);
    SumData<LongPointData> sumData =
        ImmutableSumData.create(
            true, AggregationTemporality.DELTA, Collections.singletonList(pointData));
    return ImmutableMetricData.createLongSum(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Long sum name",
        "Long sum description",
        "ms",
        sumData);
  }

  @NotNull
  private static MetricData makeDoubleSum(TraceFlags flags) {
    DoublePointData doublePointData = makeDoublePointData(flags);
    SumData<DoublePointData> sumData =
        ImmutableSumData.create(
            true, AggregationTemporality.DELTA, Collections.singletonList(doublePointData));

    return ImmutableMetricData.createDoubleSum(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Double sum name",
        "Double sum description",
        "ms",
        sumData);
  }

  @NotNull
  private static DoublePointData makeDoublePointData(TraceFlags flags) {
    return ImmutableDoublePointData.create(
        1L, 2L, TestData.ATTRIBUTES, 1.0, makeDoubleExemplars(flags));
  }

  @NotNull
  private static MetricData makeExponentialHistogram(TraceFlags flags) {
    ExponentialHistogramBuckets positiveBucket =
        ImmutableExponentialHistogramBuckets.create(1, 10, Arrays.asList(1L, 10L));
    ExponentialHistogramBuckets negativeBucket =
        ImmutableExponentialHistogramBuckets.create(1, 0, Collections.emptyList());

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
            makeDoubleExemplars(flags));

    ExponentialHistogramData histogramData =
        ImmutableExponentialHistogramData.create(
            AggregationTemporality.CUMULATIVE, Collections.singletonList(pointData));

    return ImmutableMetricData.createExponentialHistogram(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Exponential histogram name",
        "Exponential histogram description",
        "ms",
        histogramData);
  }

  @NotNull
  private static MetricData makeHistogram(TraceFlags flags) {
    HistogramPointData dataPoint =
        ImmutableHistogramPointData.create(
            1L,
            2L,
            TestData.ATTRIBUTES,
            15.0,
            true,
            4.0,
            true,
            7.0,
            Collections.singletonList(10.0),
            Arrays.asList(1L, 2L),
            makeDoubleExemplars(flags));

    HistogramData data =
        ImmutableHistogramData.create(
            AggregationTemporality.CUMULATIVE, Collections.singletonList(dataPoint));

    return ImmutableMetricData.createDoubleHistogram(
        TestData.RESOURCE_FULL,
        TestData.INSTRUMENTATION_SCOPE_INFO_FULL,
        "Histogram name",
        "Histogram description",
        "ms",
        data);
  }

  @NotNull
  private static LongPointData makeLongPointData(TraceFlags flags) {
    LongExemplarData longExemplarData = makeLongExemplarData(flags);
    return ImmutableLongPointData.create(
        1L, 2L, TestData.ATTRIBUTES, 1L, Collections.singletonList(longExemplarData));
  }

  @NotNull
  private static SpanContext makeContext(TraceFlags flags) {
    return SpanContext.create(TestData.TRACE_ID, TestData.SPAN_ID, flags, TraceState.getDefault());
  }

  private static Metric mapToProto(MetricData source) {
    return MetricDataMapper.getInstance().mapToProto(source);
  }

  private static MetricData mapToSdk(
      Metric source, Resource resource, InstrumentationScopeInfo scope) {
    return MetricDataMapper.getInstance().mapToSdk(source, resource, scope);
  }
}
