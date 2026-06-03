/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metrics;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.concurrent.Immutable;

@Immutable
@AutoValue
abstract class MetricData implements io.opentelemetry.sdk.metrics.data.MetricData {

  static MetricData createMetricData(
      Resource resource,
      InstrumentationScopeInfo instrumentationScopeInfo,
      String name,
      String description,
      String unit,
      MetricDataType type,
      Data<?> data) {
    return new AutoValue_MetricData(
        resource, instrumentationScopeInfo, name, description, unit, type, data);
  }
}
