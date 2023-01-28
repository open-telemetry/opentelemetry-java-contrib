/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming;

import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.END_OF_MAJOR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.END_OF_MINOR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SerialGcMemoryMetricTest {

  @RegisterExtension
  JfrExtension jfrExtension =
      new JfrExtension(
          builder -> builder.disableAllFeatures().enableFeature(JfrFeature.GC_DURATION_METRICS));

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
    jfrExtension.waitAndAssertMetrics(
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
