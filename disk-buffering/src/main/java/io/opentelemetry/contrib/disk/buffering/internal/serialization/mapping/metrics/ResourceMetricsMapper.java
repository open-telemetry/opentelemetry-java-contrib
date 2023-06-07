package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ResourceMetrics;
import io.opentelemetry.sdk.resources.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {ResourceMapper.class})
public interface ResourceMetricsMapper {

  ResourceMetricsMapper INSTANCE = Mappers.getMapper(ResourceMetricsMapper.class);

  @ResourceMapping
  @Mapping(target = "scopeMetrics", ignore = true)
  ResourceMetrics resourceMetricsToJson(Resource resource);
}
