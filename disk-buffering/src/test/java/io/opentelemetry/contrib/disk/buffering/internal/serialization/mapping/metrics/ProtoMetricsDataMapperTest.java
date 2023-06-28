/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.MetricDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.GaugeDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.LongPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.LongExemplarDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoMetricsDataMapperTest {

  private static final LongExemplarData LONG_EXEMPLAR_DATA =
      LongExemplarDataImpl.builder()
          .setValue(1L)
          .setEpochNanos(100L)
          .setFilteredAttributes(TestData.ATTRIBUTES)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .build();

  private static final LongPointData LONG_POINT_DATA =
      LongPointDataImpl.builder()
          .setValue(1L)
          .setAttributes(TestData.ATTRIBUTES)
          .setEpochNanos(2L)
          .setStartEpochNanos(1L)
          .setExemplars(Collections.singletonList(LONG_EXEMPLAR_DATA))
          .build();
  private static final GaugeData<LongPointData> LONG_GAUGE_DATA =
      GaugeDataImpl.LongData.builder()
          .setPoints(Collections.singletonList(LONG_POINT_DATA))
          .build();
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

  private static final MetricData OTHER_LONG_GAUGE_METRIC =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setName("Long gauge name")
          .setDescription("Long gauge description")
          .setUnit("ms")
          .setType(MetricDataType.LONG_GAUGE)
          .setData(LONG_GAUGE_DATA)
          .build();

  private static final MetricData LONG_GAUGE_METRIC_WITH_DIFFERENT_SCOPE_SAME_RESOURCE =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
          .setName("Long gauge name")
          .setDescription("Long gauge description")
          .setUnit("ms")
          .setType(MetricDataType.LONG_GAUGE)
          .setData(LONG_GAUGE_DATA)
          .build();

  private static final MetricData LONG_GAUGE_METRIC_WITH_DIFFERENT_RESOURCE =
      MetricDataImpl.builder()
          .setResource(TestData.RESOURCE_WITHOUT_SCHEMA_URL)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
          .setName("Long gauge name")
          .setDescription("Long gauge description")
          .setUnit("ms")
          .setType(MetricDataType.LONG_GAUGE)
          .setData(LONG_GAUGE_DATA)
          .build();

  @Test
  public void verifyConversionDataStructure() {
    List<MetricData> signals = Collections.singletonList(LONG_GAUGE_METRIC);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.getResourceMetricsList();
    assertEquals(1, resourceMetrics.size());
    assertEquals(1, resourceMetrics.get(0).getScopeMetricsList().size());
    assertEquals(1, resourceMetrics.get(0).getScopeMetricsList().get(0).getMetricsList().size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  public void verifyMultipleMetricsWithSameResourceAndScope() {
    List<MetricData> signals = Arrays.asList(LONG_GAUGE_METRIC, OTHER_LONG_GAUGE_METRIC);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.getResourceMetricsList();
    assertEquals(1, resourceMetrics.size());
    List<ScopeMetrics> scopeMetrics = resourceMetrics.get(0).getScopeMetricsList();
    assertEquals(1, scopeMetrics.size());
    List<Metric> metrics = scopeMetrics.get(0).getMetricsList();
    assertEquals(2, metrics.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  public void verifyMultipleMetricsWithSameResourceDifferentScope() {
    List<MetricData> signals =
        Arrays.asList(LONG_GAUGE_METRIC, LONG_GAUGE_METRIC_WITH_DIFFERENT_SCOPE_SAME_RESOURCE);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.getResourceMetricsList();
    assertEquals(1, resourceMetrics.size());
    List<ScopeMetrics> scopeMetrics = resourceMetrics.get(0).getScopeMetricsList();
    assertEquals(2, scopeMetrics.size());
    ScopeMetrics firstScope = scopeMetrics.get(0);
    ScopeMetrics secondScope = scopeMetrics.get(1);
    List<Metric> firstScopeMetrics = firstScope.getMetricsList();
    List<Metric> secondScopeMetrics = secondScope.getMetricsList();
    assertEquals(1, firstScopeMetrics.size());
    assertEquals(1, secondScopeMetrics.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  public void verifyMultipleMetricsWithDifferentResource() {
    List<MetricData> signals =
        Arrays.asList(LONG_GAUGE_METRIC, LONG_GAUGE_METRIC_WITH_DIFFERENT_RESOURCE);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.getResourceMetricsList();
    assertEquals(2, resourceMetrics.size());
    ResourceMetrics firstResourceMetrics = resourceMetrics.get(0);
    ResourceMetrics secondResourceMetrics = resourceMetrics.get(1);
    List<ScopeMetrics> firstScopeMetrics = firstResourceMetrics.getScopeMetricsList();
    List<ScopeMetrics> secondScopeMetrics = secondResourceMetrics.getScopeMetricsList();
    assertEquals(1, firstScopeMetrics.size());
    assertEquals(1, secondScopeMetrics.size());
    ScopeMetrics firstScope = firstScopeMetrics.get(0);
    ScopeMetrics secondScope = secondScopeMetrics.get(0);
    List<Metric> firstMetrics = firstScope.getMetricsList();
    List<Metric> secondMetrics = secondScope.getMetricsList();
    assertEquals(1, firstMetrics.size());
    assertEquals(1, secondMetrics.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  private static MetricsData mapToProto(Collection<MetricData> signals) {
    return ProtoMetricsDataMapper.INSTANCE.toProto(signals);
  }

  private static List<MetricData> mapFromProto(MetricsData protoData) {
    return ProtoMetricsDataMapper.INSTANCE.fromProto(protoData);
  }
}
