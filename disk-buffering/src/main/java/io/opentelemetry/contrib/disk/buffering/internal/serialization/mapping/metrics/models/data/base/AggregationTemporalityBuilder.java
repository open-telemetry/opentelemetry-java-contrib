/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;

public interface AggregationTemporalityBuilder<T extends AggregationTemporalityBuilder<?>> {

  @CanIgnoreReturnValue
  T setAggregationTemporality(AggregationTemporality value);
}
