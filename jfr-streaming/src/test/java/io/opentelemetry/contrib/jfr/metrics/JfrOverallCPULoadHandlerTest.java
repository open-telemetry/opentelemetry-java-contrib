/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_UTILIZATION;

import org.junit.jupiter.api.Test;

public class JfrOverallCPULoadHandlerTest extends AbstractJfrTest {

  @Test
  public void shouldHaveCPULoadEvents() throws Exception {
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.cpu.utilization")
                .hasUnit(UNIT_UTILIZATION)
                .hasDescription("Recent CPU utilization for the process")
                .hasDoubleGaugeSatisfying(gauge -> {}),
        metric ->
            metric
                .hasName("process.runtime.jvm.system.cpu.utilization")
                .hasUnit(UNIT_UTILIZATION)
                .hasDescription("Recent CPU utilization for the whole system")
                .hasDoubleGaugeSatisfying(gauge -> {}));
  }
}
