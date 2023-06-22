/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ResourceSignals;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ResourceSignalsData;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ScopeJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ScopeSignals;
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
    DTO_ITEM,
    DTO_SCOPE extends ScopeSignals<DTO_ITEM>,
    DTO_RESOURCE extends ResourceSignals<DTO_SCOPE>,
    DTO_RESOURCE_DATA extends ResourceSignalsData<DTO_RESOURCE>> {

  public DTO_RESOURCE_DATA toDtoItems(Collection<SDK_ITEM> sourceItems) {
    Map<Resource, DTO_RESOURCE> itemsByResourceAndScope = new HashMap<>();
    Map<InstrumentationScopeInfo, DTO_SCOPE> scopeInfoToScopeSignals = new HashMap<>();
    sourceItems.forEach(
        sourceData -> {
          Resource resource = getResource(sourceData);
          InstrumentationScopeInfo instrumentationScopeInfo =
              getInstrumentationScopeInfo(sourceData);

          DTO_RESOURCE itemsByResource = itemsByResourceAndScope.get(resource);
          if (itemsByResource == null) {
            itemsByResource = resourceSignalToDto(resource);
            itemsByResourceAndScope.put(resource, itemsByResource);
          }

          DTO_SCOPE scopeSignals = scopeInfoToScopeSignals.get(instrumentationScopeInfo);
          if (scopeSignals == null) {
            scopeSignals = instrumentationScopeToDto(instrumentationScopeInfo);
            scopeInfoToScopeSignals.put(instrumentationScopeInfo, scopeSignals);
            itemsByResource.addScopeSignalsItem(scopeSignals);
          }

          scopeSignals.addSignalItem(signalItemToDto(sourceData));
        });

    return createResourceData(itemsByResourceAndScope.values());
  }

  public List<SDK_ITEM> fromDtoItems(DTO_RESOURCE_DATA dtoResourceData) {
    List<SDK_ITEM> result = new ArrayList<>();
    for (ResourceSignals<? extends ScopeSignals<DTO_ITEM>> resourceSignal :
        dtoResourceData.getResourceSignals()) {
      Resource resource =
          ResourceMapper.INSTANCE.jsonToResource(
              Objects.requireNonNull(resourceSignal.resource), resourceSignal.schemaUrl);
      for (ScopeSignals<DTO_ITEM> scopeSignals : resourceSignal.getScopeSignals()) {
        InstrumentationScopeInfo scopeInfo =
            jsonToInstrumentationScopeInfo(
                Objects.requireNonNull(scopeSignals.scope), scopeSignals.schemaUrl);
        for (DTO_ITEM item : scopeSignals.getSignalItems()) {
          result.add(dtoToSignalItem(item, resource, scopeInfo));
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

  protected abstract DTO_ITEM signalItemToDto(SDK_ITEM sourceData);

  protected abstract DTO_RESOURCE resourceSignalToDto(Resource resource);

  protected abstract DTO_SCOPE instrumentationScopeToDto(
      InstrumentationScopeInfo instrumentationScopeInfo);

  protected abstract SDK_ITEM dtoToSignalItem(
      DTO_ITEM dtoItem, Resource resource, InstrumentationScopeInfo scopeInfo);

  protected abstract DTO_RESOURCE_DATA createResourceData(Collection<DTO_RESOURCE> items);

  protected abstract Resource getResource(SDK_ITEM source);

  protected abstract InstrumentationScopeInfo getInstrumentationScopeInfo(SDK_ITEM source);
}
