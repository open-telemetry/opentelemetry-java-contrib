/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoMetricsDataMapperTest {

  @Test
  void verifyConversionDataStructure() {
    MetricData gauge1 = TestData.makeLongGauge(TraceFlags.getDefault());
    List<MetricData> signals = Collections.singletonList(gauge1);
    MetricData expectedGauge1 = TestData.makeLongGauge(TraceFlags.getSampled());
    List<MetricData> expectedSignals = Collections.singletonList(expectedGauge1);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.resource_metrics;
    assertEquals(1, resourceMetrics.size());
    assertEquals(1, resourceMetrics.get(0).scope_metrics.size());
    assertEquals(1, resourceMetrics.get(0).scope_metrics.get(0).metrics.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(expectedSignals);
  }

  @Test
  void verifyMultipleMetricsWithSameResourceAndScope() {
    MetricData gauge1 = TestData.makeLongGauge(TraceFlags.getDefault());
    MetricData gauge2 = TestData.makeLongGauge(TraceFlags.getDefault());
    List<MetricData> signals = Arrays.asList(gauge1, gauge2);
    MetricData expectedGauge1 = TestData.makeLongGauge(TraceFlags.getSampled());
    MetricData expectedGauge2 = TestData.makeLongGauge(TraceFlags.getSampled());
    List<MetricData> expectedSignals = Arrays.asList(expectedGauge1, expectedGauge2);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.resource_metrics;
    assertEquals(1, resourceMetrics.size());
    List<ScopeMetrics> scopeMetrics = resourceMetrics.get(0).scope_metrics;
    assertEquals(1, scopeMetrics.size());
    List<Metric> metrics = scopeMetrics.get(0).metrics;
    assertEquals(2, metrics.size());

    List<MetricData> result = mapFromProto(proto);

    assertThat(result).containsExactlyInAnyOrderElementsOf(expectedSignals);
  }

  @Test
  void verifyMultipleMetricsWithSameResourceDifferentScope() {
    MetricData gauge1 = TestData.makeLongGauge(TraceFlags.getDefault());
    MetricData gauge2 =
        TestData.makeLongGauge(
            TraceFlags.getDefault(), TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION);

    MetricData expectedGauge1 = TestData.makeLongGauge(TraceFlags.getSampled());
    MetricData expectedGauge2 =
        TestData.makeLongGauge(
            TraceFlags.getSampled(), TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION);

    List<MetricData> signals = Arrays.asList(gauge1, gauge2);
    List<MetricData> expectedSignals = Arrays.asList(expectedGauge1, expectedGauge2);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.resource_metrics;
    assertEquals(1, resourceMetrics.size());
    List<ScopeMetrics> scopeMetrics = resourceMetrics.get(0).scope_metrics;
    assertEquals(2, scopeMetrics.size());
    ScopeMetrics firstScope = scopeMetrics.get(0);
    ScopeMetrics secondScope = scopeMetrics.get(1);
    List<Metric> firstScopeMetrics = firstScope.metrics;
    List<Metric> secondScopeMetrics = secondScope.metrics;
    assertEquals(1, firstScopeMetrics.size());
    assertEquals(1, secondScopeMetrics.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(expectedSignals);
  }

  @Test
  void verifyMultipleMetricsWithDifferentResource() {
    MetricData gauge1 = TestData.makeLongGauge(TraceFlags.getDefault());
    MetricData gauge2 =
        TestData.makeLongGauge(
            TraceFlags.getDefault(),
            TestData.RESOURCE_WITHOUT_SCHEMA_URL,
            TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION);
    List<MetricData> signals = Arrays.asList(gauge1, gauge2);
    MetricData expectedGauge1 = TestData.makeLongGauge(TraceFlags.getSampled());
    MetricData expectedGauge2 =
        TestData.makeLongGauge(
            TraceFlags.getSampled(),
            TestData.RESOURCE_WITHOUT_SCHEMA_URL,
            TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION);
    List<MetricData> expectedSignals = Arrays.asList(expectedGauge1, expectedGauge2);
    //    , LONG_GAUGE_METRIC_WITH_DIFFERENT_RESOURCE);
    //    List<MetricData> expectedSignals = Arrays.asList(expected);

    MetricsData proto = mapToProto(signals);

    List<ResourceMetrics> resourceMetrics = proto.resource_metrics;
    assertEquals(2, resourceMetrics.size());
    ResourceMetrics firstResourceMetrics = resourceMetrics.get(0);
    ResourceMetrics secondResourceMetrics = resourceMetrics.get(1);
    List<ScopeMetrics> firstScopeMetrics = firstResourceMetrics.scope_metrics;
    List<ScopeMetrics> secondScopeMetrics = secondResourceMetrics.scope_metrics;
    assertEquals(1, firstScopeMetrics.size());
    assertEquals(1, secondScopeMetrics.size());
    ScopeMetrics firstScope = firstScopeMetrics.get(0);
    ScopeMetrics secondScope = secondScopeMetrics.get(0);
    List<Metric> firstMetrics = firstScope.metrics;
    List<Metric> secondMetrics = secondScope.metrics;
    assertEquals(1, firstMetrics.size());
    assertEquals(1, secondMetrics.size());

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(expectedSignals);
  }

  private static MetricsData mapToProto(Collection<MetricData> signals) {
    return ProtoMetricsDataMapper.getInstance().toProto(signals);
  }

  private static List<MetricData> mapFromProto(MetricsData protoData) {
    return ProtoMetricsDataMapper.getInstance().fromProto(protoData);
  }
}
