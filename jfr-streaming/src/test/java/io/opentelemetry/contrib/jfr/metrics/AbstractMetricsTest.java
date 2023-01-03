/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.assertj.MetricAssert;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;

public class AbstractMetricsTest {

  static SdkMeterProvider meterProvider;
  static InMemoryMetricReader metricReader;
  static boolean isInitialized = false;
  static HashSet<String> garbageCollectors = new HashSet<String>();

  @BeforeAll
  static void initializeOpenTelemetry() {
    if (isInitialized) {
      return;
    }
    isInitialized = true;
    metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    GlobalOpenTelemetry.set(OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build());
    JfrMetrics.enable(meterProvider);

    for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
      garbageCollectors.add(bean.getName());
    }
  }

  @SafeVarargs
  protected final void waitAndAssertMetrics(Consumer<MetricAssert>... assertions) {
    await()
        .untilAsserted(
            () -> {
              Collection<MetricData> metrics = metricReader.collectAllMetrics();

              assertThat(metrics).isNotEmpty();

              for (Consumer<MetricAssert> assertion : assertions) {
                assertThat(metrics).anySatisfy(metric -> assertion.accept(assertThat(metric)));
              }
            });
  }
}
