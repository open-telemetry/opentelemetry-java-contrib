/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import org.assertj.core.api.Assertions;

public class MetricAssert {

  private final MetricData metric;
  private final int pointOffset;

  public MetricAssert(MetricData metric, int pointOffset) {
    this.metric = metric;
    this.pointOffset = pointOffset;
  }

  static MetricAssert assertThatMetric(MetricData metric, int pointOffset) {
    return new MetricAssert(metric, pointOffset);
  }

  MetricAssert hasName(String name) {
    Assertions.assertThat(metric.getName()).isEqualTo(name);
    return this;
  }

  MetricAssert hasValue(long value) {
    Assertions.assertThat(
            ((LongPointData) metric.getLongGaugeData().getPoints().toArray()[this.pointOffset])
                .getValue())
        .isEqualTo(value);
    return this;
  }
}
