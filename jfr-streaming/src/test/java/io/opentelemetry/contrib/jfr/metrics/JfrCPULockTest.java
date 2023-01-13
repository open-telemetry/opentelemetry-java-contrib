/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;

import org.junit.jupiter.api.Test;

public class JfrCPULockTest extends AbstractJfrTest {

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
}
