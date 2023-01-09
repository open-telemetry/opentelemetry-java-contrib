/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_CODE_CACHE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_INIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_INIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.junit.jupiter.api.Test;

class MemoryInitMetricTest extends AbstractMetricsTest {

  @Test
  void shouldHaveMemoryInitMetrics() {
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY_INIT)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_INIT)
                .hasUnit(BYTES)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      assertThat(sumData.getPoints())
                          .map(LongPointData.class::cast)
                          .anyMatch(
                              pointData ->
                                  pointData.getValue() > 0
                                      && pointData.getAttributes().equals(ATTR_CODE_CACHE));
                    }));
  }
}
