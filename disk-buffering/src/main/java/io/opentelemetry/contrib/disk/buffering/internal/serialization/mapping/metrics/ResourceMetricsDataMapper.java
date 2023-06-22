/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.BaseResourceSignalsDataMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ResourceMetrics;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ResourceMetricsData;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ScopeMetrics;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;

public final class ResourceMetricsDataMapper
    extends BaseResourceSignalsDataMapper<
        MetricData, MetricDataJson, ScopeMetrics, ResourceMetrics, ResourceMetricsData> {

  public static final ResourceMetricsDataMapper INSTANCE = new ResourceMetricsDataMapper();

  private ResourceMetricsDataMapper() {}

  @Override
  protected MetricDataJson signalItemToDto(MetricData sourceData) {
    return MetricMapper.INSTANCE.metricToJson(sourceData);
  }

  @Override
  protected ResourceMetrics resourceSignalToDto(Resource resource) {
    return ResourceMetricsMapper.INSTANCE.resourceMetricsToJson(resource);
  }

  @Override
  protected ScopeMetrics instrumentationScopeToDto(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    return ScopeMetricsMapper.INSTANCE.scopeInfoToJson(instrumentationScopeInfo);
  }

  @Override
  protected MetricData dtoToSignalItem(
      MetricDataJson jsonItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return MetricMapper.INSTANCE.jsonMetricToMetric(jsonItem, resource, scopeInfo);
  }

  @Override
  protected ResourceMetricsData createResourceData(Collection<ResourceMetrics> items) {
    return new ResourceMetricsData(items);
  }

  @Override
  protected Resource getResource(MetricData metricData) {
    return metricData.getResource();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeInfo(MetricData metricData) {
    return metricData.getInstrumentationScopeInfo();
  }
}
