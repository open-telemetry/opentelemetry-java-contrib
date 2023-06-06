package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.spans;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.common.BaseResourceSignalsDataMapper;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans.ResourceSpans;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans.ResourceSpansData;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans.ScopeSpan;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans.SpanDataJson;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;

public final class ResourceSpansDataMapper
    extends BaseResourceSignalsDataMapper<
            SpanData, SpanDataJson, ScopeSpan, ResourceSpans, ResourceSpansData> {

  public static final ResourceSpansDataMapper INSTANCE = new ResourceSpansDataMapper();

  private ResourceSpansDataMapper() {}

  @Override
  protected SpanDataJson signalItemToJson(SpanData sourceData) {
    return SpanDataMapper.INSTANCE.spanDataToJson(sourceData);
  }

  @Override
  protected ResourceSpans resourceSignalToJson(Resource resource) {
    return ResourceSpansMapper.INSTANCE.resourceSpansToJson(resource);
  }

  @Override
  protected ScopeSpan instrumentationScopeToJson(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    return ScopeSpansMapper.INSTANCE.scopeInfoToJson(instrumentationScopeInfo);
  }

  @Override
  protected SpanData jsonToSignalItem(
      SpanDataJson jsonItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return SpanDataMapper.INSTANCE.jsonToSpanData(jsonItem, resource, scopeInfo);
  }

  @Override
  protected ResourceSpansData createResourceData(Collection<ResourceSpans> items) {
    return new ResourceSpansData(items);
  }

  @Override
  protected Resource getResource(SpanData spanData) {
    return spanData.getResource();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeInfo(SpanData spanData) {
    return spanData.getInstrumentationScopeInfo();
  }
}
