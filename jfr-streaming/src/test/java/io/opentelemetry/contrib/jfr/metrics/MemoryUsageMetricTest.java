/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_COMPRESSED_CLASS_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_G1_EDEN_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_G1_SURVIVOR_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_JAVA_HEAP_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_METASPACE;
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

class MemoryUsageMetricTest extends AbstractMetricsTest {

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
    if (garbageCollectors.contains("G1 Young Generation")) {
      // Test to make sure there's metric data for both eden and survivor spaces.
      // TODO: once G1 old gen usage added to jdk.G1HeapSummary (in JDK 21), test for it here too.
      check(
          metricData -> {
            SumData<?> sumData = metricData.getLongSumData();
            assertThat(sumData.getPoints())
                .anyMatch(p -> p.getAttributes().equals(ATTR_G1_EDEN_SPACE))
                .anyMatch(p -> p.getAttributes().equals(ATTR_G1_SURVIVOR_SPACE));
          });
    } else if (garbageCollectors.contains("PS Scavenge")) {
      check(
          metricData -> {
            SumData<?> sumData = metricData.getLongSumData();
            assertThat(sumData.getPoints())
                .anyMatch(p -> p.getAttributes().equals(ATTR_PS_EDEN_SPACE))
                .anyMatch(p -> p.getAttributes().equals(ATTR_PS_SURVIVOR_SPACE))
                .anyMatch(p -> p.getAttributes().equals(ATTR_PS_OLD_GEN));
          });
    } else if (garbageCollectors.contains("Copy")) {
      // TODO: once more fine grained data is supported by JFR, this should test for young and old
      // space attributes.
      check(
          metricData -> {
            SumData<?> sumData = metricData.getLongSumData();
            assertThat(sumData.getPoints())
                .anyMatch(p -> p.getAttributes().equals(ATTR_JAVA_HEAP_SPACE));
          });
    }
    // Memory spaces in metaspace usage test
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
                          .anyMatch(p -> p.getAttributes().equals(ATTR_METASPACE));
                    }),
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      assertThat(sumData.getPoints())
                          .anyMatch(p -> p.getAttributes().equals(ATTR_COMPRESSED_CLASS_SPACE));
                    }));
  }
}
