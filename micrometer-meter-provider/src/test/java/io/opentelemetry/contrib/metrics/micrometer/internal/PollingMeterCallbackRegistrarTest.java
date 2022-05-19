/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.contrib.metrics.micrometer.CallbackRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PollingMeterCallbackRegistrarTest {

  SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  void createsPollingMeterOnCallbackRegistration() {
    Meter pollingMeter;
    try (PollingMeterCallbackRegistrar underTest =
        new PollingMeterCallbackRegistrar(() -> meterRegistry)) {

      pollingMeter = meterRegistry.find("otel_polling_meter").meter();
      assertThat(pollingMeter).isNull();

      underTest.registerCallback(() -> {});

      pollingMeter = meterRegistry.find("otel_polling_meter").meter();
      assertThat(pollingMeter).isNotNull();
    }
    pollingMeter = meterRegistry.find("otel_polling_meter").meter();
    assertThat(pollingMeter).isNull();
  }

  @Test
  void pollingMeterInvokesCallback() {
    Meter pollingMeter;
    try (PollingMeterCallbackRegistrar underTest =
        new PollingMeterCallbackRegistrar(() -> meterRegistry)) {

      pollingMeter = meterRegistry.find("otel_polling_meter").meter();
      assertThat(pollingMeter).isNull();

      Runnable callback = mock(Runnable.class);
      try (CallbackRegistration registration = underTest.registerCallback(callback)) {

        pollingMeter = meterRegistry.find("otel_polling_meter").meter();
        assertThat(pollingMeter).isNotNull();

        verifyNoInteractions(callback);

        pollingMeter.measure().forEach(measurement -> {});
        verify(callback).run();
      }

      pollingMeter.measure().forEach(measurement -> {});
      verifyNoMoreInteractions(callback);
    }
  }
}
