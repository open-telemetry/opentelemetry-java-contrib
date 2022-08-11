/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

public class MetricsSetupExtension extends BaseSetupExtension {
  static SdkMeterProvider meterProvider;

  @Override
  void setup() {
    InMemoryMetricReader metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    GlobalOpenTelemetry.set(OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build());
    JfrMetrics.enable(meterProvider);
    AbstractMetricsTest.setReader(metricReader);
  }

  @Override
  public void close() throws Throwable {}
}
