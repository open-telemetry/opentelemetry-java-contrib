/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans.ResourceSpans;
import io.opentelemetry.sdk.resources.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ResourceMapper.class})
public interface ResourceSpansMapper {

  ResourceSpansMapper INSTANCE = new ResourceSpansMapperImpl();

  @ResourceMapping
  @Mapping(target = "scopeSpans", ignore = true)
  ResourceSpans resourceSpansToJson(Resource resource);
}
