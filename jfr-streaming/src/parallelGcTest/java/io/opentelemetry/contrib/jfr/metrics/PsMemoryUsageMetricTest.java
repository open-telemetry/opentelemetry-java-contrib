/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_PS_EDEN_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_PS_OLD_GEN;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_PS_SURVIVOR_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_AFTER;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class PsMemoryUsageMetricTest extends AbstractMetricsTest {

  private void check(ThrowingConsumer<MetricData> attributeCheck) {
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY)
                .satisfies(attributeCheck),
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY_AFTER)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
                .satisfies(attributeCheck));
  }

  /**
   * This is a basic test for process.runtime.jvm.memory.usage and
   * process.runtime.jvm.memory.usage_after_last_gc metrics.
   */
  @Test
  void shouldHaveMemoryUsageMetrics() {
    System.gc();
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints())
              .anyMatch(p -> p.getAttributes().equals(ATTR_PS_EDEN_SPACE))
              .anyMatch(p -> p.getAttributes().equals(ATTR_PS_SURVIVOR_SPACE))
              .anyMatch(p -> p.getAttributes().equals(ATTR_PS_OLD_GEN));
        });
  }
}
