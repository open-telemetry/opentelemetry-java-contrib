package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.common;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ResourceSignals;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ResourceSignalsData;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ScopeJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ScopeSignals;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfoBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public abstract class BaseResourceSignalsDataMapper<
    SDK_ITEM,
    JSON_ITEM,
    JSON_SCOPE extends ScopeSignals<JSON_ITEM>,
    JSON_RESOURCE extends ResourceSignals<JSON_SCOPE>,
    JSON_RESOURCE_DATA extends ResourceSignalsData<JSON_RESOURCE>> {

  public JSON_RESOURCE_DATA toJsonDto(List<SDK_ITEM> sourceItems) {
    Map<Resource, JSON_RESOURCE> itemsByResourceAndScope = new HashMap<>();
    Map<InstrumentationScopeInfo, JSON_SCOPE> scopeInfoToScopeSignals = new HashMap<>();
    sourceItems.forEach(
        sourceData -> {
          Resource resource = getResource(sourceData);
          InstrumentationScopeInfo instrumentationScopeInfo =
              getInstrumentationScopeInfo(sourceData);

          JSON_RESOURCE itemsByResource = itemsByResourceAndScope.get(resource);
          if (itemsByResource == null) {
            itemsByResource = resourceSignalToJson(resource);
            itemsByResourceAndScope.put(resource, itemsByResource);
          }

          JSON_SCOPE scopeSignals = scopeInfoToScopeSignals.get(instrumentationScopeInfo);
          if (scopeSignals == null) {
            scopeSignals = instrumentationScopeToJson(instrumentationScopeInfo);
            scopeInfoToScopeSignals.put(instrumentationScopeInfo, scopeSignals);
            itemsByResource.addScopeSignalsItem(scopeSignals);
          }

          scopeSignals.addSignalItem(signalItemToJson(sourceData));
        });

    return createResourceData(itemsByResourceAndScope.values());
  }

  public List<SDK_ITEM> fromJsonDto(JSON_RESOURCE_DATA json) {
    List<SDK_ITEM> result = new ArrayList<>();
    for (ResourceSignals<? extends ScopeSignals<JSON_ITEM>> resourceSignal :
        json.getResourceSignals()) {
      Resource resource =
          ResourceMapper.INSTANCE.jsonToResource(
              Objects.requireNonNull(resourceSignal.resource), resourceSignal.schemaUrl);
      for (ScopeSignals<JSON_ITEM> scopeSignals : resourceSignal.getScopeSignals()) {
        InstrumentationScopeInfo scopeInfo =
            jsonToInstrumentationScopeInfo(
                Objects.requireNonNull(scopeSignals.scope), scopeSignals.schemaUrl);
        for (JSON_ITEM item : scopeSignals.getSignalItems()) {
          result.add(jsonToSignalItem(item, resource, scopeInfo));
        }
      }
    }

    return result;
  }

  private static InstrumentationScopeInfo jsonToInstrumentationScopeInfo(
      ScopeJson scope, @Nullable String schemaUrl) {
    InstrumentationScopeInfoBuilder builder =
        InstrumentationScopeInfo.builder(Objects.requireNonNull(scope.name));
    if (scope.version != null) {
      builder.setVersion(scope.version);
    }
    if (schemaUrl != null) {
      builder.setSchemaUrl(schemaUrl);
    }
    builder.setAttributes(scope.attributes);
    return builder.build();
  }

  protected abstract JSON_ITEM signalItemToJson(SDK_ITEM sourceData);

  protected abstract JSON_RESOURCE resourceSignalToJson(Resource resource);

  protected abstract JSON_SCOPE instrumentationScopeToJson(
      InstrumentationScopeInfo instrumentationScopeInfo);

  protected abstract SDK_ITEM jsonToSignalItem(
      JSON_ITEM jsonItem, Resource resource, InstrumentationScopeInfo scopeInfo);

  protected abstract JSON_RESOURCE_DATA createResourceData(Collection<JSON_RESOURCE> items);

  protected abstract Resource getResource(SDK_ITEM source);

  protected abstract InstrumentationScopeInfo getInstrumentationScopeInfo(SDK_ITEM source);
}
