/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerMeterProviderTest {
  SimpleMeterRegistry meterRegistry;

  CallbackRegistrar callbackRegistrar;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbackRegistrar = new TestCallbackRegistrar(Collections.emptyList());
  }

  @Test
  void createMeter() {
    MeterProvider underTest =
        MicrometerMeterProvider.builder(meterRegistry)
            .setCallbackRegistrar(callbackRegistrar)
            .build();
    Meter meter = underTest.get("name");

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeNameTag())
                  .isEqualTo(Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "name"));
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersionTag())
                  .isEqualTo(Constants.UNKNOWN_INSTRUMENTATION_VERSION_TAG);
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isNull();
            });
  }

  @Test
  void createMeterWithNameAndVersion() {
    MeterProvider underTest =
        MicrometerMeterProvider.builder(meterRegistry)
            .setCallbackRegistrar(callbackRegistrar)
            .build();
    Meter meter = underTest.meterBuilder("name").setInstrumentationVersion("version").build();

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeNameTag())
                  .isEqualTo(Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "name"));
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersionTag())
                  .isEqualTo(Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "version"));
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isNull();
            });
  }

  @Test
  void createMeterWithNameAndSchemaUrl() {
    MeterProvider underTest =
        MicrometerMeterProvider.builder(meterRegistry)
            .setCallbackRegistrar(callbackRegistrar)
            .build();
    Meter meter = underTest.meterBuilder("name").setSchemaUrl("schemaUrl").build();

    assertThat(meter)
        .isInstanceOfSatisfying(
            MicrometerMeter.class,
            micrometerMeter -> {
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeNameTag())
                  .isEqualTo(Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "name"));
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersionTag())
                  .isEqualTo(Constants.UNKNOWN_INSTRUMENTATION_VERSION_TAG);
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isEqualTo("schemaUrl");
            });
  }

  @Test
  void createMeterWithNameVersionAndSchemaUrl() {
    MeterProvider underTest =
        MicrometerMeterProvider.builder(meterRegistry)
            .setCallbackRegistrar(callbackRegistrar)
            .build();
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
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeNameTag())
                  .isEqualTo(Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "name"));
              assertThat(micrometerMeter.meterSharedState.instrumentationScopeVersionTag())
                  .isEqualTo(Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "version"));
              assertThat(micrometerMeter.meterSharedState.schemaUrl()).isEqualTo("schemaUrl");
            });
  }

  @Test
  void close() {
    CallbackRegistrar registrar = mock(CallbackRegistrar.class);
    MicrometerMeterProvider underTest =
        MicrometerMeterProvider.builder(meterRegistry).setCallbackRegistrar(registrar).build();

    underTest.close();
    verify(registrar).close();
  }
}
