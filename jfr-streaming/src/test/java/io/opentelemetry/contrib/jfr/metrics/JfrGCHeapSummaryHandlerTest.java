/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_AFTER;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

public class JfrGCHeapSummaryHandlerTest extends AbstractMetricsTest {

  static class AttributeCheck implements ThrowingConsumer<MetricData> {
    @Override
    public void acceptThrows(MetricData metricData) throws Throwable {
      SumData<?> sumData = metricData.getLongSumData();
      assertThat(sumData.getPoints())
          .map(LongPointData.class::cast)
          .anyMatch(
              p ->
                  p.getAttributes().get(ATTR_TYPE).equals(HEAP)
                      && p.getAttributes().get(ATTR_POOL).equals("Java heap space"));
    }
  }

  @Test
  public void shouldHaveGCEvents()
      throws Exception { // TODO Remove this test. Its replaced by MemoryUsageMetricTest

    System.gc();

    waitAndAssertMetrics(
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY)
                .satisfies(new AttributeCheck()),
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY_AFTER)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
                .satisfies(new AttributeCheck()),
        metric ->
            metric
                .hasName(METRIC_NAME_COMMITTED)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_COMMITTED)
                .satisfies(new AttributeCheck()));
  }
}
