/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class JfrCPUTest extends AbstractMetricsTest {

  @Test
  public void shouldHaveGcAndLockEvents() throws Exception {
    // This should generate some events
    System.gc();
    synchronized (this) {
      Thread.sleep(1000);
    }

    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("runtime.jvm.cpu.longlock.time")
                .hasUnit(MILLISECONDS)
                .hasDoubleHistogram(),
        metric ->
            metric
                .hasName("runtime.jvm.gc.duration")
                .hasUnit(MILLISECONDS)
                .hasDoubleHistogram()
                .points()
                .anySatisfy(point -> assertThat(point.getCount() > 0)));
  }
}
