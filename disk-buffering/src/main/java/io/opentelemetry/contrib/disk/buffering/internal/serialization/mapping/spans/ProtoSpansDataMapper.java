package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.BaseProtoSignalsDataMapper;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.TracesData;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtoSpansDataMapper
    extends BaseProtoSignalsDataMapper<SpanData, Span, TracesData, ResourceSpans, ScopeSpans> {

  public static final ProtoSpansDataMapper INSTANCE = new ProtoSpansDataMapper();

  @Override
  protected Span signalItemToProto(SpanData sourceData) {
    return SpanDataMapper.INSTANCE.mapToProto(sourceData);
  }

  @Override
  protected List<ResourceSpans> getProtoResources(TracesData protoData) {
    return protoData.getResourceSpansList();
  }

  @Override
  protected SpanData protoToSignalItem(
      Span protoSignalItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return SpanDataMapper.INSTANCE.mapToSdk(protoSignalItem, resource, scopeInfo);
  }

  @Override
  protected TracesData createProtoData(
      Map<Resource, Map<InstrumentationScopeInfo, List<Span>>> itemsByResource) {
    List<ResourceSpans> items = new ArrayList<>();
    itemsByResource.forEach(
        (resource, instrumentationScopeInfoScopedSpansMap) -> {
          ResourceSpans.Builder resourceSpansBuilder = createProtoResourceBuilder(resource);
          for (Map.Entry<InstrumentationScopeInfo, List<Span>> spansByScope :
              instrumentationScopeInfoScopedSpansMap.entrySet()) {
            ScopeSpans.Builder scopeBuilder = createProtoScopeBuilder(spansByScope.getKey());
            scopeBuilder.addAllSpans(spansByScope.getValue());
            resourceSpansBuilder.addScopeSpans(scopeBuilder.build());
          }
          items.add(resourceSpansBuilder.build());
        });
    return TracesData.newBuilder().addAllResourceSpans(items).build();
  }

  @Override
  protected List<Span> getSignalsFromProto(ScopeSpans scopeSignals) {
    return scopeSignals.getSpansList();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeFromProto(ScopeSpans scopeSignals) {
    return protoToInstrumentationScopeInfo(scopeSignals.getScope(), scopeSignals.getSchemaUrl());
  }

  @Override
  protected List<ScopeSpans> getScopes(ResourceSpans resourceSignal) {
    return resourceSignal.getScopeSpansList();
  }

  @Override
  protected Resource getResourceFromProto(ResourceSpans resourceSignal) {
    return protoToResource(resourceSignal.getResource(), resourceSignal.getSchemaUrl());
  }

  @Override
  protected Resource getResourceFromSignal(SpanData source) {
    return source.getResource();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeInfo(SpanData source) {
    return source.getInstrumentationScopeInfo();
  }

  private ResourceSpans.Builder createProtoResourceBuilder(Resource resource) {
    ResourceSpans.Builder builder =
        ResourceSpans.newBuilder().setResource(resourceToProto(resource));
    if (resource.getSchemaUrl() != null) {
      builder.setSchemaUrl(resource.getSchemaUrl());
    }
    return builder;
  }

  private ScopeSpans.Builder createProtoScopeBuilder(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    ScopeSpans.Builder builder =
        ScopeSpans.newBuilder().setScope(instrumentationScopeToProto(instrumentationScopeInfo));
    if (instrumentationScopeInfo.getSchemaUrl() != null) {
      builder.setSchemaUrl(instrumentationScopeInfo.getSchemaUrl());
    }
    return builder;
  }
}
