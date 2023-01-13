/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import org.junit.jupiter.api.Test;

class SerialMemoryUsageMetricTest extends AbstractMetricsTest {

  /**
   * This is a basic test for process.runtime.jvm.memory.usage and
   * process.runtime.jvm.memory.usage_after_last_gc metrics.
   */
  @Test
  void shouldHaveMemoryUsageMetrics() {
    System.gc();
    // TODO: needs JFR support. Placeholder.
  }
}
