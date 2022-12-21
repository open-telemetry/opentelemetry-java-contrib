/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_UTILIZATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OverallCPULoadHandlerTest extends AbstractMetricsTest {

  private void check(String name, String description) {
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName(name)
                .hasUnit(UNIT_UTILIZATION)
                .hasDescription(description)
                .hasDoubleGaugeSatisfying(
                    gauge ->
                        gauge.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData -> {
                                      assertThat(pointData.getValue()).isGreaterThan(0);
                                      assertThat(pointData.getValue()).isLessThan(1.0);
                                    }))));
  }

  @Test
  public void shouldHaveCPULoadEvents() {
    check(
        "process.runtime.jvm.system.cpu.load_1m",
        "Average CPU load of the whole system for the last minute");
    check(
        "process.runtime.jvm.system.cpu.utilization",
        "Recent CPU utilization for the whole system");
    check("process.runtime.jvm.cpu.utilization", "Recent CPU utilization for the process");
  }
}
