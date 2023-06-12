/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ScopeMapping;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.ScopeMetrics;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ScopeMetricsMapper {

  ScopeMetricsMapper INSTANCE = new ScopeMetricsMapperImpl();

  @ScopeMapping
  @Mapping(target = "metrics", ignore = true)
  ScopeMetrics scopeInfoToJson(InstrumentationScopeInfo source);
}
