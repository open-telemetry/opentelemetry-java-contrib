/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.ScopeLogs;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ScopeLogsMapper {

  ScopeLogsMapper INSTANCE = new ScopeLogsMapperImpl();

  @ScopeMapping
  @Mapping(target = "logRecords", ignore = true)
  ScopeLogs scopeInfoToJson(InstrumentationScopeInfo source);
}
