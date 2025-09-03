/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.metrics.micrometer.internal.instruments.MicrometerDoubleGauge;
import io.opentelemetry.contrib.metrics.micrometer.internal.instruments.MicrometerDoubleHistogram;
import io.opentelemetry.contrib.metrics.micrometer.internal.instruments.MicrometerLongCounter;
import io.opentelemetry.contrib.metrics.micrometer.internal.instruments.MicrometerLongUpDownCounter;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;

final class MicrometerMeter implements Meter {
  final MeterSharedState meterSharedState;

  MicrometerMeter(MeterSharedState meterSharedState) {
    this.meterSharedState = meterSharedState;
  }

  @Override
  public LongCounterBuilder counterBuilder(String name) {
    requireNonNull(name, "name");
    return MicrometerLongCounter.builder(meterSharedState, name);
  }

  @Override
  public LongUpDownCounterBuilder upDownCounterBuilder(String name) {
    requireNonNull(name, "name");
    return MicrometerLongUpDownCounter.builder(meterSharedState, name);
  }

  @Override
  public DoubleHistogramBuilder histogramBuilder(String name) {
    requireNonNull(name, "name");
    return MicrometerDoubleHistogram.builder(meterSharedState, name);
  }

  @Override
  public DoubleGaugeBuilder gaugeBuilder(String name) {
    requireNonNull(name, "name");
    return MicrometerDoubleGauge.builder(meterSharedState, name);
  }
}
