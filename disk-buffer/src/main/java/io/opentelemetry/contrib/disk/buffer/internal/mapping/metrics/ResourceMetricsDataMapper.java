package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.common.BaseResourceSignalsDataMapper;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.ResourceMetrics;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.ResourceMetricsData;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.ScopeMetrics;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;

public class ResourceMetricsDataMapper
    extends BaseResourceSignalsDataMapper<
        MetricData, MetricDataJson, ScopeMetrics, ResourceMetrics, ResourceMetricsData> {

  public static final ResourceMetricsDataMapper INSTANCE = new ResourceMetricsDataMapper();

  private ResourceMetricsDataMapper() {}

  @Override
  protected MetricDataJson signalItemToJson(MetricData sourceData) {
    return MetricMapper.INSTANCE.metricToJson(sourceData);
  }

  @Override
  protected ResourceMetrics resourceSignalToJson(Resource resource) {
    return ResourceMetricsMapper.INSTANCE.resourceMetricsToJson(resource);
  }

  @Override
  protected ScopeMetrics instrumentationScopeToJson(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    return ScopeMetricsMapper.INSTANCE.scopeInfoToJson(instrumentationScopeInfo);
  }

  @Override
  protected MetricData jsonToSignalItem(
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
