/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ResourceMetrics;
import io.opentelemetry.sdk.resources.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ResourceMapper.class})
public interface ResourceMetricsMapper {

  ResourceMetricsMapper INSTANCE = new ResourceMetricsMapperImpl();

  @ResourceMapping
  @Mapping(target = "scopeMetrics", ignore = true)
  ResourceMetrics resourceMetricsToJson(Resource resource);
}
