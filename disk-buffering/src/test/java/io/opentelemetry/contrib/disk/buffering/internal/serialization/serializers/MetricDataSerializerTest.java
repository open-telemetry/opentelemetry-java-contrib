/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.MetricDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.SumDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.SummaryDataImpl;
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
import io.opentelemetry.sdk.metrics.data.MetricDataType;
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
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableValueAtQuantile;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class MetricDataSerializerTest extends BaseSignalSerializerTest<MetricData> {

  private static final LongExemplarData LONG_EXEMPLAR_DATA =
      ImmutableLongExemplarData.create(TestData.ATTRIBUTES, 100L, TestData.SPAN_CONTEXT, 1L);

  private static final DoubleExemplarData DOUBLE_EXEMPLAR_DATA =
      ImmutableDoubleExemplarData.create(TestData.ATTRIBUTES, 100L, TestData.SPAN_CONTEXT, 1.0);
  private static final LongPointData LONG_POINT_DATA =
      ImmutableLongPointData.create(
          1L, 2L, TestData.ATTRIBUTES, 1L, Collections.singletonList(LONG_EXEMPLAR_DATA));

  private static final DoublePointData DOUBLE_POINT_DATA =
      ImmutableDoublePointData.create(
          1L, 2L, TestData.ATTRIBUTES, 1.0, Collections.singletonList(DOUBLE_EXEMPLAR_DATA));

  private static final GaugeData<LongPointData> LONG_GAUGE_DATA =
      ImmutableGaugeData.create(Collections.singletonList(LONG_POINT_DATA));

  private static final GaugeData<DoublePointData> DOUBLE_GAUGE_DATA =
      ImmutableGaugeData.create(Collections.singletonList(DOUBLE_POINT_DATA));

  private static final SumData<LongPointData> LONG_SUM_DATA =
      SumDataImpl.LongData.builder()
          .setMonotonic(true)
          .setAggregationTemporality(AggregationTemporality.DELTA)
          .setPoints(Collections.singletonList(LONG_POINT_DATA))
          .build();

  private static final SumData<DoublePointData> DOUBLE_SUM_DATA =
      SumDataImpl.DoubleData.builder()
          .setMonotonic(true)
          .setAggregationTemporality(AggregationTemporality.DELTA)
          .setPoints(Collections.singletonList(DOUBLE_POINT_DATA))
          .build();

  private static final ValueAtQuantile VALUE_AT_QUANTILE =
      ImmutableValueAtQuantile.create(2.0, 1.0);
  private static final SummaryPointData SUMMARY_POINT_DATA =
      ImmutableSummaryPointData.create(
          1L, 2L, TestData.ATTRIBUTES, 1L, 2.0, Collections.singletonList(VALUE_AT_QUANTILE));

  private static final SummaryData SUMMARY_DATA =
      SummaryDataImpl.builder().setPoints(Collections.singletonList(SUMMARY_POINT_DATA)).build();

  private static final HistogramPointData HISTOGRAM_POINT_DATA =
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
          Collections.singletonList(DOUBLE_EXEMPLAR_DATA));
  private static final ExponentialHistogramBuckets POSITIVE_BUCKET =
      ImmutableExponentialHistogramBuckets.create(1, 10, Arrays.asList(1L, 10L));

  private static final ExponentialHistogramBuckets NEGATIVE_BUCKET =
      ImmutableExponentialHistogramBuckets.create(1, 0, Collections.emptyList());
  private static final ExponentialHistogramPointData EXPONENTIAL_HISTOGRAM_POINT_DATA =
      ImmutableExponentialHistogramPointData.create(
          1,
          10.0,
          1L,
          true,
          2.0,
          true,
          4.0,
          POSITIVE_BUCKET,
          NEGATIVE_BUCKET,
          1L,
          2L,
          TestData.ATTRIBUTES,
          Collections.singletonList(DOUBLE_EXEMPLAR_DATA));
  private static final HistogramData HISTOGRAM_DATA =
      ImmutableHistogramData.create(
          AggregationTemporality.CUMULATIVE, Collections.singletonList(HISTOGRAM_POINT_DATA));
  private static final ExponentialHistogramData EXPONENTIAL_HISTOGRAM_DATA =
      ImmutableExponentialHistogramData.create(
          AggregationTemporality.CUMULATIVE,
          Collections.singletonList(EXPONENTIAL_HISTOGRAM_POINT_DATA));
  private static final MetricData LONG_GAUGE_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Long gauge name")
          .setDescription("Long gauge description")
          .setUnit("ms")
          .setType(MetricDataType.LONG_GAUGE)
          .setData(LONG_GAUGE_DATA)
          .build();

  private static final MetricData DOUBLE_GAUGE_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Double gauge name")
          .setDescription("Double gauge description")
          .setUnit("ms")
          .setType(MetricDataType.DOUBLE_GAUGE)
          .setData(DOUBLE_GAUGE_DATA)
          .build();

  private static final MetricData LONG_SUM_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Long sum name")
          .setDescription("Long sum description")
          .setUnit("ms")
          .setType(MetricDataType.LONG_SUM)
          .setData(LONG_SUM_DATA)
          .build();

  private static final MetricData DOUBLE_SUM_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Double sum name")
          .setDescription("Double sum description")
          .setUnit("ms")
          .setType(MetricDataType.DOUBLE_SUM)
          .setData(DOUBLE_SUM_DATA)
          .build();

  private static final MetricData SUMMARY_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Summary name")
          .setDescription("Summary description")
          .setUnit("ms")
          .setType(MetricDataType.SUMMARY)
          .setData(SUMMARY_DATA)
          .build();

  private static final MetricData HISTOGRAM_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Histogram name")
          .setDescription("Histogram description")
          .setUnit("ms")
          .setType(MetricDataType.HISTOGRAM)
          .setData(HISTOGRAM_DATA)
          .build();

  private static final MetricData EXPONENTIAL_HISTOGRAM_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Exponential histogram name")
          .setDescription("Exponential histogram description")
          .setUnit("ms")
          .setType(MetricDataType.EXPONENTIAL_HISTOGRAM)
          .setData(EXPONENTIAL_HISTOGRAM_DATA)
          .build();

  @Test
  public void verifySerialization() {
    assertSerialization(
        LONG_GAUGE_METRIC,
        DOUBLE_GAUGE_METRIC,
        LONG_SUM_METRIC,
        DOUBLE_SUM_METRIC,
        SUMMARY_METRIC,
        HISTOGRAM_METRIC,
        EXPONENTIAL_HISTOGRAM_METRIC);
  }

  @Override
  protected SignalSerializer<MetricData> getSerializer() {
    return SignalSerializer.ofMetrics();
  }
}
