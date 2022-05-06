/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerMeterProviderTest {
  SimpleMeterRegistry meterRegistry;

  List<Runnable> callbacks;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new CopyOnWriteArrayList<>();
  }

  @Test
  void createMeter() {
    MeterProvider underTest = new MicrometerMeterProvider(meterRegistry, callbacks);
    Meter meter = underTest.get("name");

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeName())
                  .isEqualTo("name");
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersion()).isNull();
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isNull();
            });
  }

  @Test
  void createMeterWithNameAndVersion() {
    MeterProvider underTest = new MicrometerMeterProvider(meterRegistry, callbacks);
    Meter meter = underTest.meterBuilder("name").setInstrumentationVersion("version").build();

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeName())
                  .isEqualTo("name");
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersion())
                  .isEqualTo("version");
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isNull();
            });
  }

  @Test
  void createMeterWithNameAndSchemaUrl() {
    MeterProvider underTest = new MicrometerMeterProvider(meterRegistry, callbacks);
    Meter meter = underTest.meterBuilder("name").setSchemaUrl("schemaUrl").build();

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeName())
                  .isEqualTo("name");
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersion()).isNull();
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isEqualTo("schemaUrl");
            });
  }

  @Test
  void createMeterWithNameVersionAndSchemaUrl() {
    MeterProvider underTest = new MicrometerMeterProvider(meterRegistry, callbacks);
    Meter meter =
        underTest
            .meterBuilder("name")
            .setInstrumentationVersion("version")
            .setSchemaUrl("schemaUrl")
            .build();

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeName())
                  .isEqualTo("name");
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersion())
                  .isEqualTo("version");
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isEqualTo("schemaUrl");
            });
  }

  @Test
  void createsPollingMeter() {
    MicrometerMeterProvider underTest = new MicrometerMeterProvider(meterRegistry, callbacks);

    io.micrometer.core.instrument.Meter meter =
        meterRegistry.find(MicrometerMeterProvider.OTEL_POLLING_METER_NAME).meter();
    assertThat(meter).isNotNull();
    assertThat(meter.getId().getName()).isEqualTo(MicrometerMeterProvider.OTEL_POLLING_METER_NAME);

    Runnable callback = mock(Runnable.class);
    callbacks.add(callback);

    meter.measure().forEach(measurement -> {});
    verify(callback).run();
    meter.measure().forEach(measurement -> {});
    verify(callback, times(2)).run();
    meter.measure().forEach(measurement -> {});
    verify(callback, times(3)).run();

    underTest.close();
    meter = meterRegistry.find(MicrometerMeterProvider.OTEL_POLLING_METER_NAME).meter();
    assertThat(meter).isNull();
  }
}
