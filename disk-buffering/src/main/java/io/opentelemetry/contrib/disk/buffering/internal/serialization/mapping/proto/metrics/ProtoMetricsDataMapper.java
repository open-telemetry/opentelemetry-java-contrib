package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.common.BaseProtoSignalsDataMapper;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtoMetricsDataMapper
    extends BaseProtoSignalsDataMapper<
        MetricData, Metric, MetricsData, ResourceMetrics, ScopeMetrics> {

  public static final ProtoMetricsDataMapper INSTANCE = new ProtoMetricsDataMapper();

  @Override
  protected Metric signalItemToProto(MetricData sourceData) {
    return MetricDataMapper.INSTANCE.mapToProto(sourceData);
  }

  @Override
  protected MetricData protoToSignalItem(
      Metric protoSignalItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return MetricDataMapper.INSTANCE.mapToSdk(protoSignalItem, resource, scopeInfo);
  }

  @Override
  protected List<ResourceMetrics> getProtoResources(MetricsData protoData) {
    return protoData.getResourceMetricsList();
  }

  @Override
  protected MetricsData createProtoData(
      Map<Resource, Map<InstrumentationScopeInfo, List<Metric>>> itemsByResource) {
    List<ResourceMetrics> items = new ArrayList<>();
    itemsByResource.forEach(
        (resource, instrumentationScopeInfoScopedMetricsMap) -> {
          ResourceMetrics.Builder resourceMetricsBuilder = createProtoResourceBuilder(resource);
          for (Map.Entry<InstrumentationScopeInfo, List<Metric>> metricsByScope :
              instrumentationScopeInfoScopedMetricsMap.entrySet()) {
            ScopeMetrics.Builder scopeBuilder = createProtoScopeBuilder(metricsByScope.getKey());
            scopeBuilder.addAllMetrics(metricsByScope.getValue());
            resourceMetricsBuilder.addScopeMetrics(scopeBuilder.build());
          }
          items.add(resourceMetricsBuilder.build());
        });
    return MetricsData.newBuilder().addAllResourceMetrics(items).build();
  }

  private ScopeMetrics.Builder createProtoScopeBuilder(InstrumentationScopeInfo scopeInfo) {
    return ScopeMetrics.newBuilder().setScope(instrumentationScopeToProto(scopeInfo));
  }

  private ResourceMetrics.Builder createProtoResourceBuilder(Resource resource) {
    return ResourceMetrics.newBuilder().setResource(resourceToProto(resource));
  }

  @Override
  protected List<Metric> getSignalsFromProto(ScopeMetrics scopeSignals) {
    return scopeSignals.getMetricsList();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeFromProto(ScopeMetrics scopeSignals) {
    return protoToInstrumentationScopeInfo(scopeSignals.getScope(), scopeSignals.getSchemaUrl());
  }

  @Override
  protected List<ScopeMetrics> getScopes(ResourceMetrics resourceSignal) {
    return resourceSignal.getScopeMetricsList();
  }

  @Override
  protected Resource getResourceFromProto(ResourceMetrics resourceSignal) {
    return protoToResource(resourceSignal.getResource(), resourceSignal.getSchemaUrl());
  }

  @Override
  protected Resource getResourceFromSignal(MetricData source) {
    return source.getResource();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeInfo(MetricData source) {
    return source.getInstrumentationScopeInfo();
  }
}
