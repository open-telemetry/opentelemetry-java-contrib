package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.common.ResourceMapper;
import io.opentelemetry.contrib.disk.buffer.internal.mapping.common.ResourceMapping;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.ResourceMetrics;
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
