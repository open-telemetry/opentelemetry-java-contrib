/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.BaseProtoSignalsDataMapper;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProtoMetricsDataMapper
    extends BaseProtoSignalsDataMapper<
        MetricData, Metric, ExportMetricsServiceRequest, ResourceMetrics, ScopeMetrics> {

  private static final ProtoMetricsDataMapper INSTANCE = new ProtoMetricsDataMapper();

  public static ProtoMetricsDataMapper getInstance() {
    return INSTANCE;
  }

  @Override
  protected Metric signalItemToProto(MetricData sourceData) {
    return MetricDataMapper.getInstance().mapToProto(sourceData);
  }

  @Override
  protected MetricData protoToSignalItem(
      Metric protoSignalItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return MetricDataMapper.getInstance().mapToSdk(protoSignalItem, resource, scopeInfo);
  }

  @Override
  protected List<ResourceMetrics> getProtoResources(ExportMetricsServiceRequest protoData) {
    return protoData.resource_metrics;
  }

  @Override
  protected ExportMetricsServiceRequest createProtoData(
      Map<Resource, Map<InstrumentationScopeInfo, List<Metric>>> itemsByResource) {
    List<ResourceMetrics> items = new ArrayList<>();
    itemsByResource.forEach(
        (resource, instrumentationScopeInfoScopedMetricsMap) -> {
          ResourceMetrics.Builder resourceMetricsBuilder = createProtoResourceBuilder(resource);
          for (Map.Entry<InstrumentationScopeInfo, List<Metric>> metricsByScope :
              instrumentationScopeInfoScopedMetricsMap.entrySet()) {
            ScopeMetrics.Builder scopeBuilder = createProtoScopeBuilder(metricsByScope.getKey());
            scopeBuilder.metrics.addAll(metricsByScope.getValue());
            resourceMetricsBuilder.scope_metrics.add(scopeBuilder.build());
          }
          items.add(resourceMetricsBuilder.build());
        });
    return new ExportMetricsServiceRequest.Builder().resource_metrics(items).build();
  }

  private ScopeMetrics.Builder createProtoScopeBuilder(InstrumentationScopeInfo scopeInfo) {
    ScopeMetrics.Builder builder =
        new ScopeMetrics.Builder().scope(instrumentationScopeToProto(scopeInfo));
    if (scopeInfo.getSchemaUrl() != null) {
      builder.schema_url(scopeInfo.getSchemaUrl());
    }
    return builder;
  }

  private ResourceMetrics.Builder createProtoResourceBuilder(Resource resource) {
    ResourceMetrics.Builder builder =
        new ResourceMetrics.Builder().resource(resourceToProto(resource));
    if (resource.getSchemaUrl() != null) {
      builder.schema_url(resource.getSchemaUrl());
    }
    return builder;
  }

  @Override
  protected List<Metric> getSignalsFromProto(ScopeMetrics scopeSignals) {
    return scopeSignals.metrics;
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeFromProto(ScopeMetrics scopeSignals) {
    return protoToInstrumentationScopeInfo(scopeSignals.scope, scopeSignals.schema_url);
  }

  @Override
  protected List<ScopeMetrics> getScopes(ResourceMetrics resourceSignal) {
    return resourceSignal.scope_metrics;
  }

  @Override
  protected Resource getResourceFromProto(ResourceMetrics resourceSignal) {
    return protoToResource(resourceSignal.resource, resourceSignal.schema_url);
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
