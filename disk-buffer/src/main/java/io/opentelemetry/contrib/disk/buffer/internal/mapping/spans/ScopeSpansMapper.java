package io.opentelemetry.contrib.disk.buffer.internal.mapping.spans;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.spans.ScopeSpan;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ScopeSpansMapper {

  ScopeSpansMapper INSTANCE = Mappers.getMapper(ScopeSpansMapper.class);

  @ScopeMapping
  @Mapping(target = "spans", ignore = true)
  ScopeSpan scopeInfoToJson(InstrumentationScopeInfo source);
}
