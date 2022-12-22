/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.SECONDS_PER_MIN;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.contrib.jfr.metrics.internal.cpu.OverallCPULoadHandler;
import org.junit.jupiter.api.Test;

public class JfrCPULockTest extends AbstractMetricsTest {
  @Test
  public void shouldHaveLockEvents() throws Exception {
    // This should generate some events
    System.gc();
    synchronized (this) {
      Thread.sleep(1000);
    }

    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.cpu.longlock")
                .hasUnit(MILLISECONDS)
                .hasHistogramSatisfying(histogram -> {}));
  }

  @Test
  public void testMovingAverage() {
    OverallCPULoadHandler handler = new OverallCPULoadHandler();
    double sum = 0;
    for (int i = 0; i < 2 * SECONDS_PER_MIN; i++) {
      double result = handler.updateMovingAverage(i);
      sum += i;
      double divisor = i + 1;
      if (i >= SECONDS_PER_MIN) {
        sum -= (i - SECONDS_PER_MIN);
        divisor = SECONDS_PER_MIN;
      }
      assertTrue(result == sum / divisor);
    }
  }
}
