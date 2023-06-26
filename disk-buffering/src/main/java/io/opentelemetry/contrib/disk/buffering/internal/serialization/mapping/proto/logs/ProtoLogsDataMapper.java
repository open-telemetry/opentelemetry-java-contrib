package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.logs;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.common.BaseProtoSignalsDataMapper;
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

  public static final ProtoLogsDataMapper INSTANCE = new ProtoLogsDataMapper();

  @Override
  protected LogRecord signalItemToProto(LogRecordData sourceData) {
    return LogRecordDataMapper.INSTANCE.mapToProto(sourceData);
  }

  @Override
  protected LogRecordData protoToSignalItem(
      LogRecord logRecord, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return LogRecordDataMapper.INSTANCE.mapToSdk(logRecord, resource, scopeInfo);
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
    return ScopeLogs.newBuilder().setScope(instrumentationScopeToProto(scopeInfo));
  }

  private ResourceLogs.Builder createProtoResourceBuilder(Resource resource) {
    return ResourceLogs.newBuilder().setResource(resourceToProto(resource));
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
