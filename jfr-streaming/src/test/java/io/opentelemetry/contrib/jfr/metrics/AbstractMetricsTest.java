/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.sun.tools.attach.VirtualMachine;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.testing.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.assertj.metrics.MetricAssertions;
import io.opentelemetry.sdk.testing.assertj.metrics.MetricDataAssert;
import java.util.Collection;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;

public class AbstractMetricsTest {

  static SdkMeterProvider meterProvider;
  static InMemoryMetricReader metricReader;

  @BeforeAll
  public static void loadAgent() throws Exception {
    var pid = "" + ProcessHandle.current().pid();
    var vm = VirtualMachine.attach(pid);
    vm.loadAgent("jfr-streaming.jar", "");
    vm.detach();
  }

  @BeforeAll
  static void initializeOpenTelemetry() {
    metricReader = new InMemoryMetricReader();
    meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader).buildAndRegisterGlobal();
  }

  protected void waitAndAssertMetrics(Consumer<MetricDataAssert>... assertions) {
    await()
        .untilAsserted(
            () -> {
              Collection<MetricData> metrics = metricReader.collectAllMetrics();

              assertThat(metrics).isNotEmpty();

              for (Consumer<MetricDataAssert> assertion : assertions) {
                assertThat(metrics)
                    .anySatisfy(metric -> assertion.accept(MetricAssertions.assertThat(metric)));
              }
            });
  }
}
