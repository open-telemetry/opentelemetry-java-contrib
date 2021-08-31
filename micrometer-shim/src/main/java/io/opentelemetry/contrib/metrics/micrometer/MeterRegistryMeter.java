/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterBuilder;

import java.util.function.Supplier;

final class MeterRegistryMeter implements Meter {
  private final SharedMeterState state;

  private MeterRegistryMeter(SharedMeterState state) {
    this.state = state;
  }

  @Override
  public LongCounterBuilder counterBuilder(String name) {
    return LongCounter.newBuilder(state, name);
  }

  @Override
  public LongUpDownCounterBuilder upDownCounterBuilder(String name) {
    return null;
  }

  @Override
  public DoubleHistogramBuilder histogramBuilder(String name) {
    return null;
  }

  @Override
  public DoubleGaugeBuilder gaugeBuilder(String name) {
    return DoubleGauge.newBuilder(state, name);
  }

  public static Builder newBuilder(
      Supplier<MeterRegistry> meterRegistrySupplier, String instrumentationName) {
    return new Builder(meterRegistrySupplier, instrumentationName);
  }

  private static final class Builder implements MeterBuilder {
    private final Supplier<MeterRegistry> meterRegistrySupplier;
    private final String instrumentationName;
    private String instrumentationVersion;
    private String schemaUrl;

    public Builder(Supplier<MeterRegistry> meterRegistrySupplier, String instrumentationName) {
      this.meterRegistrySupplier = meterRegistrySupplier;
      this.instrumentationName = instrumentationName;
    }

    @Override
    public MeterBuilder setSchemaUrl(String schemaUrl) {
      this.schemaUrl = schemaUrl;
      return this;
    }

    @Override
    public MeterBuilder setInstrumentationVersion(String instrumentationVersion) {
      this.instrumentationVersion = instrumentationVersion;
      return this;
    }

    @Override
    public Meter build() {
      MeterCallbackFactory callbackFactory = new MeterCallbackFactory(meterRegistrySupplier);
      SharedMeterState state =
          new SharedMeterState(
              meterRegistrySupplier, instrumentationName, instrumentationVersion, schemaUrl, callbackFactory);
      return new MeterRegistryMeter(state);
    }
  }
}
