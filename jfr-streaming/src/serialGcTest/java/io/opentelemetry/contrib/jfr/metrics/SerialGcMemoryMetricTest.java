/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.END_OF_MAJOR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.END_OF_MINOR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.junit.jupiter.api.Test;

class SerialGcMemoryMetricTest extends AbstractJfrTest {
  @Test
  void shouldHaveMemoryLimitMetrics() {
    // TODO: needs JFR support. Placeholder.
  }

  @Test
  void shouldHaveMemoryUsageMetrics() {
    System.gc();
    // TODO: needs JFR support. Placeholder.
  }

  @Test
  void shouldHaveGCDurationMetrics() throws Exception {
    // TODO: Need a reliable way to test old and young gen GC in isolation.
    System.gc();
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.gc.duration")
                .hasUnit(MILLISECONDS)
                .hasDescription(METRIC_DESCRIPTION_GC_DURATION)
                .satisfies(
                    metricData -> {
                      HistogramData data = metricData.getHistogramData();
                      assertThat(data.getPoints())
                          .map(HistogramPointData.class::cast)
                          .anyMatch(
                              p ->
                                  p.getSum() > 0
                                      && (p.getAttributes()
                                              .equals(
                                                  Attributes.of(
                                                      ATTR_GC,
                                                      "Copy",
                                                      ATTR_ACTION,
                                                      END_OF_MINOR_GC))
                                          || p.getAttributes()
                                              .equals(
                                                  Attributes.of(
                                                      ATTR_GC,
                                                      "MarkSweepCompact",
                                                      ATTR_ACTION,
                                                      END_OF_MAJOR_GC))));
                    }));
  }
}
