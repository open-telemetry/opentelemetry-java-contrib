/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans.ScopeSpan;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ScopeSpansMapper {

  ScopeSpansMapper INSTANCE = new ScopeSpansMapperImpl();

  @ScopeMapping
  @Mapping(target = "spans", ignore = true)
  ScopeSpan scopeInfoToJson(InstrumentationScopeInfo source);
}
