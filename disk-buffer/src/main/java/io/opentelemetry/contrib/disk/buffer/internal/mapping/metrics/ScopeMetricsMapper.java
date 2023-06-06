package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.ScopeMetrics;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ScopeMetricsMapper {

  ScopeMetricsMapper INSTANCE = Mappers.getMapper(ScopeMetricsMapper.class);

  @ScopeMapping
  @Mapping(target = "metrics", ignore = true)
  ScopeMetrics scopeInfoToJson(InstrumentationScopeInfo source);
}
