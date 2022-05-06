/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MicrometerMeterTest {
  SimpleMeterRegistry meterRegistry;

  List<Runnable> callbacks;

  Meter underTest;

  io.micrometer.core.instrument.Meter pollingMeter;

  @Mock Consumer<ObservableLongMeasurement> longMeasurementConsumer;

  @Mock Consumer<ObservableDoubleMeasurement> doubleMeasurementConsumer;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new CopyOnWriteArrayList<>();
    MeterProvider meterProvider = new MicrometerMeterProvider(meterRegistry, callbacks);
    underTest = meterProvider.get("meter");
    pollingMeter = meterRegistry.find(MicrometerMeterProvider.OTEL_POLLING_METER_NAME).meter();
  }

  @Test
  void observesCounterOnPoll() {
    ObservableLongCounter counter =
        underTest.counterBuilder("counter").buildWithCallback(longMeasurementConsumer);

    assertThat(callbacks).isNotEmpty();
    verifyNoInteractions(longMeasurementConsumer);

    pollingMeter.measure().forEach(measurement -> {});
    verify(longMeasurementConsumer).accept(any());
    pollingMeter.measure().forEach(measurement -> {});
    verify(longMeasurementConsumer, times(2)).accept(any());
    pollingMeter.measure().forEach(measurement -> {});
    verify(longMeasurementConsumer, times(3)).accept(any());

    counter.close();
    assertThat(callbacks).isEmpty();
  }

  @Test
  void observesUpDownCounterOnPoll() {
    ObservableLongUpDownCounter counter =
        underTest.upDownCounterBuilder("upDownCounter").buildWithCallback(longMeasurementConsumer);

    assertThat(callbacks).isNotEmpty();
    verifyNoInteractions(longMeasurementConsumer);

    pollingMeter.measure().forEach(measurement -> {});
    verify(longMeasurementConsumer).accept(any());
    pollingMeter.measure().forEach(measurement -> {});
    verify(longMeasurementConsumer, times(2)).accept(any());
    pollingMeter.measure().forEach(measurement -> {});
    verify(longMeasurementConsumer, times(3)).accept(any());

    counter.close();
    assertThat(callbacks).isEmpty();
  }

  @Test
  void observesGaugeOnPoll() {
    ObservableDoubleGauge counter =
        underTest.gaugeBuilder("gauge").buildWithCallback(doubleMeasurementConsumer);

    assertThat(callbacks).isNotEmpty();
    verify(doubleMeasurementConsumer).accept(any());

    pollingMeter.measure().forEach(measurement -> {});
    verify(doubleMeasurementConsumer, times(2)).accept(any());
    pollingMeter.measure().forEach(measurement -> {});
    verify(doubleMeasurementConsumer, times(3)).accept(any());
    pollingMeter.measure().forEach(measurement -> {});
    verify(doubleMeasurementConsumer, times(4)).accept(any());

    counter.close();
    assertThat(callbacks).isEmpty();
  }
}
