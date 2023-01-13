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
import io.opentelemetry.sdk.metrics.data.PointData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

class PsGcDurationMetricTest extends AbstractMetricsTest {
  static class AttributeCheck implements ThrowingConsumer<PointData> {
    @Override
    public void acceptThrows(PointData pointData) throws Throwable {
      if (pointData.getAttributes().get(ATTR_ACTION).equals(END_OF_MINOR_GC)) {
        assertThat(pointData.getAttributes())
            .isEqualTo(Attributes.of(ATTR_GC, "PS Scavenge", ATTR_ACTION, END_OF_MINOR_GC));
      } else {
        assertThat(pointData.getAttributes())
            .isEqualTo(Attributes.of(ATTR_GC, "PS MarkSweep", ATTR_ACTION, END_OF_MAJOR_GC));
      }
    }
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
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point -> point.hasSumGreaterThan(0).satisfies(new AttributeCheck()))));
  }
}
