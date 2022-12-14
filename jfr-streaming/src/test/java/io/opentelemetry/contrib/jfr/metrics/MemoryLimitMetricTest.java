/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_LIMIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NON_HEAP;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class MemoryLimitMetricTest extends AbstractMetricsTest {
  private void check(ThrowingConsumer<MetricData> attributeCheck) {
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.memory.limit")
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_LIMIT)
                .satisfies(attributeCheck));
  }

  @Test
  void shouldHaveMemoryLimitMetrics() throws Exception {

    System.gc();

    if (HandlerRegistry.garbageCollectors.contains("G1 Young Generation")) {
      // TODO: needs JFR support

    } else if (HandlerRegistry.garbageCollectors.contains("PS Scavenge")) {
      check(
          metricData -> {
            SumData<?> sumData = metricData.getLongSumData();
            assertThat(sumData.getPoints())
                .anyMatch(
                    p ->
                        p.getAttributes()
                            .equals(Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Eden Space")))
                .anyMatch(
                    p ->
                        p.getAttributes()
                            .equals(Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Survivor Space")))
                .anyMatch(
                    p ->
                        p.getAttributes()
                            .equals(Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Old Gen")));
          });
    } else if (HandlerRegistry.garbageCollectors.contains("Copy")) {
      // TODO: Needs JFR support for more fine grained memory pools
    }

    // Metaspace related:
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints())
              .anyMatch(
                  p ->
                      p.getAttributes()
                          .equals(
                              Attributes.of(
                                  ATTR_TYPE, NON_HEAP, ATTR_POOL, "Compressed Class Space")));
        });
  }
}
