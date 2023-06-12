/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models;

import io.opentelemetry.sdk.metrics.data.AggregationTemporality;

public final class Constants {
  public static final AggregationTemporality DEFAULT_AGGREGATION_TEMPORALITY =
      AggregationTemporality.DELTA;

  private Constants() {}
}
