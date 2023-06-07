package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ScopeMetrics;
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