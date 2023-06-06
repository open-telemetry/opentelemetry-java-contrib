package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.logs;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.logs.ScopeLogs;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ScopeLogsMapper {

  ScopeLogsMapper INSTANCE = Mappers.getMapper(ScopeLogsMapper.class);

  @ScopeMapping
  @Mapping(target = "logRecords", ignore = true)
  ScopeLogs scopeInfoToJson(InstrumentationScopeInfo source);
}
