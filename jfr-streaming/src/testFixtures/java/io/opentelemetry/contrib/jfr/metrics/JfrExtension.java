/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.assertj.MetricAssert;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.util.Collection;
import java.util.function.Consumer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class JfrExtension implements BeforeEachCallback, AfterEachCallback {

  private SdkMeterProvider meterProvider;
  private InMemoryMetricReader metricReader;
  private JfrTelemetry jfrTelemetry;

  @Override
  public void beforeEach(ExtensionContext context) {
    metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();
    jfrTelemetry = JfrTelemetry.create(sdk);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    meterProvider.close();
    jfrTelemetry.close();
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
