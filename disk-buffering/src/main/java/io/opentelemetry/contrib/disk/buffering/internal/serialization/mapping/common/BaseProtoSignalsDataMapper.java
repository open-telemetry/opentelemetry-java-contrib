/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfoBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public abstract class BaseProtoSignalsDataMapper<
    SIGNAL_ITEM, PROTO_SIGNAL_ITEM, PROTO_DATA, PROTO_RESOURCE_ITEM, PROTO_SCOPE_ITEM> {

  public PROTO_DATA toProto(Collection<SIGNAL_ITEM> sourceItems) {
    Map<Resource, Map<InstrumentationScopeInfo, List<PROTO_SIGNAL_ITEM>>> itemsByResourceAndScope =
        new HashMap<>();
    sourceItems.forEach(
        sourceData -> {
          Resource resource = getResourceFromSignal(sourceData);
          InstrumentationScopeInfo instrumentationScopeInfo =
              getInstrumentationScopeInfo(sourceData);

          Map<InstrumentationScopeInfo, List<PROTO_SIGNAL_ITEM>> itemsByResource =
              itemsByResourceAndScope.get(resource);
          if (itemsByResource == null) {
            itemsByResource = new HashMap<>();
            itemsByResourceAndScope.put(resource, itemsByResource);
          }

          List<PROTO_SIGNAL_ITEM> scopeSignals = itemsByResource.get(instrumentationScopeInfo);
          if (scopeSignals == null) {
            scopeSignals = new ArrayList<>();
            itemsByResource.put(instrumentationScopeInfo, scopeSignals);
          }

          scopeSignals.add(signalItemToProto(sourceData));
        });

    return createProtoData(itemsByResourceAndScope);
  }

  public List<SIGNAL_ITEM> fromProto(PROTO_DATA protoData) {
    List<SIGNAL_ITEM> result = new ArrayList<>();
    for (PROTO_RESOURCE_ITEM resourceSignal : getProtoResources(protoData)) {
      Resource resource = getResourceFromProto(resourceSignal);
      for (PROTO_SCOPE_ITEM scopeSignals : getScopes(resourceSignal)) {
        InstrumentationScopeInfo scopeInfo = getInstrumentationScopeFromProto(scopeSignals);
        for (PROTO_SIGNAL_ITEM item : getSignalsFromProto(scopeSignals)) {
          result.add(protoToSignalItem(item, resource, scopeInfo));
        }
      }
    }

    return result;
  }

  protected io.opentelemetry.proto.resource.v1.Resource resourceToProto(Resource resource) {
    return ResourceMapper.getInstance().mapToProto(resource);
  }

  protected Resource protoToResource(
      io.opentelemetry.proto.resource.v1.Resource protoResource, String schemaUrl) {
    return ResourceMapper.getInstance()
        .mapToSdk(protoResource, schemaUrl.isEmpty() ? null : schemaUrl);
  }

  protected InstrumentationScopeInfo protoToInstrumentationScopeInfo(
      InstrumentationScope scope, @Nullable String schemaUrl) {
    InstrumentationScopeInfoBuilder builder = InstrumentationScopeInfo.builder(scope.name);
    builder.setAttributes(protoToAttributes(scope.attributes));
    if (!scope.version.isEmpty()) {
      builder.setVersion(scope.version);
    }
    if (schemaUrl != null) {
      builder.setSchemaUrl(schemaUrl);
    }
    return builder.build();
  }

  protected InstrumentationScope instrumentationScopeToProto(InstrumentationScopeInfo source) {
    InstrumentationScope.Builder builder =
        new InstrumentationScope.Builder().name(source.getName());
    if (source.getVersion() != null) {
      builder.version(source.getVersion());
    }
    builder.attributes.addAll(attributesToProto(source.getAttributes()));
    return builder.build();
  }

  protected abstract PROTO_SIGNAL_ITEM signalItemToProto(SIGNAL_ITEM sourceData);

  protected abstract SIGNAL_ITEM protoToSignalItem(
      PROTO_SIGNAL_ITEM protoSignalItem, Resource resource, InstrumentationScopeInfo scopeInfo);

  protected abstract List<PROTO_RESOURCE_ITEM> getProtoResources(PROTO_DATA protoData);

  protected abstract PROTO_DATA createProtoData(
      Map<Resource, Map<InstrumentationScopeInfo, List<PROTO_SIGNAL_ITEM>>> itemsByResource);

  protected abstract List<PROTO_SIGNAL_ITEM> getSignalsFromProto(PROTO_SCOPE_ITEM scopeSignals);

  protected abstract InstrumentationScopeInfo getInstrumentationScopeFromProto(
      PROTO_SCOPE_ITEM scopeSignals);

  protected abstract List<PROTO_SCOPE_ITEM> getScopes(PROTO_RESOURCE_ITEM resourceSignal);

  protected abstract Resource getResourceFromProto(PROTO_RESOURCE_ITEM resourceSignal);

  protected abstract Resource getResourceFromSignal(SIGNAL_ITEM source);

  protected abstract InstrumentationScopeInfo getInstrumentationScopeInfo(SIGNAL_ITEM source);

  private static List<KeyValue> attributesToProto(Attributes source) {
    return AttributesMapper.getInstance().attributesToProto(source);
  }

  private static Attributes protoToAttributes(List<KeyValue> source) {
    return AttributesMapper.getInstance().protoToAttributes(source);
  }
}
