/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.END_OF_MINOR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class GCDurationMetricTest extends AbstractMetricsTest {
  static class AttributeCheck implements ThrowingConsumer<PointData> {
    @Override
    public void acceptThrows(PointData pointData) throws Throwable {
      // Each point must have attributes of one of the following variation:
      if (HandlerRegistry.garbageCollectors.contains("G1 Young Generation")) {
        assertThat(pointData.getAttributes())
            .isEqualTo(Attributes.of(ATTR_GC, "G1 Young Generation", ATTR_ACTION, END_OF_MINOR_GC));
      } else if (HandlerRegistry.garbageCollectors.contains("PS Scavenge")) {
        assertThat(pointData.getAttributes())
            .isEqualTo(Attributes.of(ATTR_GC, "PS Scavenge", ATTR_ACTION, END_OF_MINOR_GC));
      } else if (HandlerRegistry.garbageCollectors.contains("Copy")) {
        assertThat(pointData.getAttributes())
            .isEqualTo(Attributes.of(ATTR_GC, "Copy", ATTR_ACTION, END_OF_MINOR_GC));
      }
    }
  }

  @Test
  void shouldHaveGCDurationMetrics() throws Exception {
    // This test is only a smoke test for young GC
    // TODO: Need a reliable way to test old gen GC.
    System.gc();
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.gc.duration")
                .hasUnit(MILLISECONDS)
                .hasDescription(METRIC_DESCRIPTION_GC_DURATION)
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point -> point.hasSumGreaterThan(0).satisfies(new AttributeCheck()))));
  }
}
