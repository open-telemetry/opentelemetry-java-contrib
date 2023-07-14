/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.BaseProtoSignalsDataMapper;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtoLogsDataMapper
    extends BaseProtoSignalsDataMapper<
        LogRecordData, LogRecord, LogsData, ResourceLogs, ScopeLogs> {

  private static final ProtoLogsDataMapper INSTANCE = new ProtoLogsDataMapper();

  public static ProtoLogsDataMapper getInstance() {
    return INSTANCE;
  }

  @Override
  protected LogRecord signalItemToProto(LogRecordData sourceData) {
    return LogRecordDataMapper.getInstance().mapToProto(sourceData);
  }

  @Override
  protected LogRecordData protoToSignalItem(
      LogRecord logRecord, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return LogRecordDataMapper.getInstance().mapToSdk(logRecord, resource, scopeInfo);
  }

  @Override
  protected List<ResourceLogs> getProtoResources(LogsData logsData) {
    return logsData.getResourceLogsList();
  }

  @Override
  protected LogsData createProtoData(
      Map<Resource, Map<InstrumentationScopeInfo, List<LogRecord>>> itemsByResource) {
    List<ResourceLogs> items = new ArrayList<>();
    itemsByResource.forEach(
        (resource, instrumentationScopeInfoScopedLogsMap) -> {
          ResourceLogs.Builder resourceLogsBuilder = createProtoResourceBuilder(resource);
          for (Map.Entry<InstrumentationScopeInfo, List<LogRecord>> logsByScope :
              instrumentationScopeInfoScopedLogsMap.entrySet()) {
            ScopeLogs.Builder scopeBuilder = createProtoScopeBuilder(logsByScope.getKey());
            scopeBuilder.addAllLogRecords(logsByScope.getValue());
            resourceLogsBuilder.addScopeLogs(scopeBuilder.build());
          }
          items.add(resourceLogsBuilder.build());
        });
    return LogsData.newBuilder().addAllResourceLogs(items).build();
  }

  private ScopeLogs.Builder createProtoScopeBuilder(InstrumentationScopeInfo scopeInfo) {
    ScopeLogs.Builder builder =
        ScopeLogs.newBuilder().setScope(instrumentationScopeToProto(scopeInfo));
    if (scopeInfo.getSchemaUrl() != null) {
      builder.setSchemaUrl(scopeInfo.getSchemaUrl());
    }
    return builder;
  }

  private ResourceLogs.Builder createProtoResourceBuilder(Resource resource) {
    ResourceLogs.Builder builder = ResourceLogs.newBuilder().setResource(resourceToProto(resource));
    if (resource.getSchemaUrl() != null) {
      builder.setSchemaUrl(resource.getSchemaUrl());
    }
    return builder;
  }

  @Override
  protected List<LogRecord> getSignalsFromProto(ScopeLogs scopeSignals) {
    return scopeSignals.getLogRecordsList();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeFromProto(ScopeLogs scopeSignals) {
    return protoToInstrumentationScopeInfo(scopeSignals.getScope(), scopeSignals.getSchemaUrl());
  }

  @Override
  protected List<ScopeLogs> getScopes(ResourceLogs resourceSignal) {
    return resourceSignal.getScopeLogsList();
  }

  @Override
  protected Resource getResourceFromProto(ResourceLogs resourceSignal) {
    return protoToResource(resourceSignal.getResource(), resourceSignal.getSchemaUrl());
  }

  @Override
  protected Resource getResourceFromSignal(LogRecordData source) {
    return source.getResource();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeInfo(LogRecordData source) {
    return source.getInstrumentationScopeInfo();
  }
}
