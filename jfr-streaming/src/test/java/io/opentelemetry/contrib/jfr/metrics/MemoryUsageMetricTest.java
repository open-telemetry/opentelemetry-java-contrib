/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_AFTER;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class MemoryUsageMetricTest extends AbstractMetricsTest {
  static class AttributeCheck implements ThrowingConsumer<PointData> {
    @Override
    public void acceptThrows(PointData pointData) throws Throwable {}
  }

  /**
   * This is a basic test for process.runtime.jvm.memory.usage and
   * process.runtime.jvm.memory.usage_after_last_gc metrics.
   */
  @Test
  void shouldHaveMemoryUsageMetrics() throws Exception {

    System.gc();
    if (HandlerRegistry.garbageCollectors.contains("G1 Young Generation")) {
      // Test to make sure there's metric data for both eden and survivor spaces.
      // TODO: once G1 old gen usage added to jdk.G1HeapSummary (in JDK 21), test for it here too.
      waitAndAssertMetrics(
          metric ->
              metric
                  .hasName(METRIC_NAME_MEMORY)
                  .hasUnit(BYTES)
                  .hasDescription(METRIC_DESCRIPTION_MEMORY)
                  .satisfies(
                      metricData -> {
                        SumData<?> sumData = metricData.getLongSumData();
                        assertThat(sumData.getPoints())
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "G1 Eden Space")))
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "G1 Survivor Space")));
                      }),
          metric ->
              metric
                  .hasName(METRIC_NAME_MEMORY_AFTER)
                  .hasUnit(BYTES)
                  .hasDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
                  .satisfies(
                      metricData -> {
                        SumData<?> sumData = metricData.getLongSumData();
                        assertThat(sumData.getPoints())
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "G1 Eden Space")))
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "G1 Survivor Space")));
                      }));

    } else if (HandlerRegistry.garbageCollectors.contains("PS Scavenge")) {
      waitAndAssertMetrics(
          metric ->
              metric
                  .hasName(METRIC_NAME_MEMORY)
                  .hasUnit(BYTES)
                  .hasDescription(METRIC_DESCRIPTION_MEMORY)
                  .satisfies(
                      metricData -> {
                        SumData<?> sumData = metricData.getLongSumData();
                        assertThat(sumData.getPoints())
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "PS Eden Space")))
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "PS Survivor Space")))
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "PS Old Gen")));
                      }),
          metric ->
              metric
                  .hasName(METRIC_NAME_MEMORY_AFTER)
                  .hasUnit(BYTES)
                  .hasDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
                  .satisfies(
                      metricData -> {
                        SumData<?> sumData = metricData.getLongSumData();
                        assertThat(sumData.getPoints())
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "PS Eden Space")))
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "PS Survivor Space")))
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "PS Old Gen")));
                      }));
    } else if (HandlerRegistry.garbageCollectors.contains("Copy")) {
      // TODO: once more fine grained data is supported by JFR, this should test for young and old
      // space attributes.
      waitAndAssertMetrics(
          metric ->
              metric
                  .hasName(METRIC_NAME_MEMORY)
                  .hasUnit(BYTES)
                  .hasDescription(METRIC_DESCRIPTION_MEMORY)
                  .satisfies(
                      metricData -> {
                        SumData<?> sumData = metricData.getLongSumData();
                        assertThat(sumData.getPoints())
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "Java heap space")));
                      }),
          metric ->
              metric
                  .hasName(METRIC_NAME_MEMORY_AFTER)
                  .hasUnit(BYTES)
                  .hasDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
                  .satisfies(
                      metricData -> {
                        SumData<?> sumData = metricData.getLongSumData();
                        assertThat(sumData.getPoints())
                            .anyMatch(
                                p ->
                                    p.getAttributes()
                                        .equals(
                                            Attributes.of(
                                                ATTR_TYPE, HEAP, ATTR_POOL, "Java heap space")));
                      }));
    }
  }
}
