/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

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

public final class ProtoSpansDataMapper
    extends BaseProtoSignalsDataMapper<SpanData, Span, TracesData, ResourceSpans, ScopeSpans> {

  private static final ProtoSpansDataMapper INSTANCE = new ProtoSpansDataMapper();

  public static ProtoSpansDataMapper getInstance() {
    return INSTANCE;
  }

  @Override
  protected Span signalItemToProto(SpanData sourceData) {
    return SpanDataMapper.getInstance().mapToProto(sourceData);
  }

  @Override
  protected List<ResourceSpans> getProtoResources(TracesData protoData) {
    return protoData.resource_spans;
  }

  @Override
  protected SpanData protoToSignalItem(
      Span protoSignalItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return SpanDataMapper.getInstance().mapToSdk(protoSignalItem, resource, scopeInfo);
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
            scopeBuilder.spans.addAll(spansByScope.getValue());
            resourceSpansBuilder.scope_spans.add(scopeBuilder.build());
          }
          items.add(resourceSpansBuilder.build());
        });
    return new TracesData.Builder().resource_spans(items).build();
  }

  @Override
  protected List<Span> getSignalsFromProto(ScopeSpans scopeSignals) {
    return scopeSignals.spans;
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeFromProto(ScopeSpans scopeSignals) {
    return protoToInstrumentationScopeInfo(scopeSignals.scope, scopeSignals.schema_url);
  }

  @Override
  protected List<ScopeSpans> getScopes(ResourceSpans resourceSignal) {
    return resourceSignal.scope_spans;
  }

  @Override
  protected Resource getResourceFromProto(ResourceSpans resourceSignal) {
    return protoToResource(resourceSignal.resource, resourceSignal.schema_url);
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
    ResourceSpans.Builder builder = new ResourceSpans.Builder().resource(resourceToProto(resource));
    if (resource.getSchemaUrl() != null) {
      builder.schema_url(resource.getSchemaUrl());
    }
    return builder;
  }

  private ScopeSpans.Builder createProtoScopeBuilder(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    ScopeSpans.Builder builder =
        new ScopeSpans.Builder().scope(instrumentationScopeToProto(instrumentationScopeInfo));
    if (instrumentationScopeInfo.getSchemaUrl() != null) {
      builder.schema_url(instrumentationScopeInfo.getSchemaUrl());
    }
    return builder;
  }
}
