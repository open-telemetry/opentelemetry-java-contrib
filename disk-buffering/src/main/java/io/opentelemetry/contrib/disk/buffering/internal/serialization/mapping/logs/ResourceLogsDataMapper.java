package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.BaseResourceSignalsDataMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.LogRecordDataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.ResourceLogs;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.ResourceLogsData;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.ScopeLogs;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;

public final class ResourceLogsDataMapper
    extends BaseResourceSignalsDataMapper<
            LogRecordData, LogRecordDataJson, ScopeLogs, ResourceLogs, ResourceLogsData> {

  public static final ResourceLogsDataMapper INSTANCE = new ResourceLogsDataMapper();

  private ResourceLogsDataMapper() {}

  @Override
  protected LogRecordDataJson signalItemToJson(LogRecordData sourceData) {
    return LogRecordMapper.INSTANCE.logToJson(sourceData);
  }

  @Override
  protected ResourceLogs resourceSignalToJson(Resource resource) {
    return ResourceLogsMapper.INSTANCE.resourceLogsToJson(resource);
  }

  @Override
  protected ScopeLogs instrumentationScopeToJson(
      InstrumentationScopeInfo instrumentationScopeInfo) {
    return ScopeLogsMapper.INSTANCE.scopeInfoToJson(instrumentationScopeInfo);
  }

  @Override
  protected LogRecordData jsonToSignalItem(
      LogRecordDataJson jsonItem, Resource resource, InstrumentationScopeInfo scopeInfo) {
    return LogRecordMapper.INSTANCE.jsonToLog(jsonItem, resource, scopeInfo);
  }

  @Override
  protected ResourceLogsData createResourceData(Collection<ResourceLogs> items) {
    return new ResourceLogsData(items);
  }

  @Override
  protected Resource getResource(LogRecordData logRecordData) {
    return logRecordData.getResource();
  }

  @Override
  protected InstrumentationScopeInfo getInstrumentationScopeInfo(LogRecordData logRecordData) {
    return logRecordData.getInstrumentationScopeInfo();
  }
}
