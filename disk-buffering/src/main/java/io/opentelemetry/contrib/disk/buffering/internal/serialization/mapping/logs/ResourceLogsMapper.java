/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ResourceMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.ResourceLogs;
import io.opentelemetry.sdk.resources.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ResourceMapper.class})
public interface ResourceLogsMapper {

  ResourceLogsMapper INSTANCE = new ResourceLogsMapperImpl();

  @ResourceMapping
  @Mapping(target = "scopeLogs", ignore = true)
  ResourceLogs resourceLogsToJson(Resource resource);
}
