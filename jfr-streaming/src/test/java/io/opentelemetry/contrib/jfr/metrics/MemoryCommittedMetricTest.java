/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_COMPRESSED_CLASS_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_G1_EDEN_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_METASPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_PS_EDEN_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_PS_OLD_GEN;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_PS_SURVIVOR_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_COMMITTED;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class MemoryCommittedMetricTest extends AbstractMetricsTest {
  private void check(ThrowingConsumer<MetricData> attributeCheck) {
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.memory.committed")
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_COMMITTED)
                .satisfies(attributeCheck));
  }

  @Test
  void shouldHaveMemoryCommittedMetrics() {
    System.gc();
    if (garbageCollectors.contains("G1 Young Generation")) {
      // TODO: need JFR support for the other G1 pools
      check(
          metricData -> {
            SumData<?> sumData = metricData.getLongSumData();
            assertThat(sumData.getPoints())
                .anyMatch(p -> p.getAttributes().equals(ATTR_G1_EDEN_SPACE));
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
      // TODO: Needs JFR support for more fine grained memory pools
    }
    // Metaspace related:
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints())
              .anyMatch(p -> p.getAttributes().equals(ATTR_COMPRESSED_CLASS_SPACE));
        });
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints()).anyMatch(p -> p.getAttributes().equals(ATTR_METASPACE));
        });
  }
}
