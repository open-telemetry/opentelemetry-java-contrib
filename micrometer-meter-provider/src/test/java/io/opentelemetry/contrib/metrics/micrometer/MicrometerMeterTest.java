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
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.contrib.metrics.micrometer.internal.instruments.MicrometerDoubleHistogram;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MicrometerMeterTest {
  SimpleMeterRegistry meterRegistry;

  List<Runnable> callbacks;

  TestCallbackRegistrar callbackRegistrar;

  Meter underTest;

  @Mock Consumer<ObservableLongMeasurement> longMeasurementConsumer;

  @Mock Consumer<ObservableDoubleMeasurement> doubleMeasurementConsumer;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new ArrayList<>();
    callbackRegistrar = new TestCallbackRegistrar(callbacks);
    MeterProvider meterProvider =
        new MicrometerMeterProvider(() -> meterRegistry, callbackRegistrar);
    underTest = meterProvider.get("meter");
  }

  @Test
  void observesCounterOnPoll() {
    ObservableLongCounter counter =
        underTest.counterBuilder("counter").buildWithCallback(longMeasurementConsumer);

    assertThat(callbacks).isNotEmpty();
    verifyNoInteractions(longMeasurementConsumer);

    callbackRegistrar.run();
    verify(longMeasurementConsumer).accept(any());
    callbackRegistrar.run();
    verify(longMeasurementConsumer, times(2)).accept(any());
    callbackRegistrar.run();
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

    callbackRegistrar.run();
    verify(longMeasurementConsumer).accept(any());
    callbackRegistrar.run();
    verify(longMeasurementConsumer, times(2)).accept(any());
    callbackRegistrar.run();
    verify(longMeasurementConsumer, times(3)).accept(any());

    counter.close();
    assertThat(callbacks).isEmpty();
  }

  @Test
  void observesGaugeOnPoll() {
    ObservableDoubleGauge counter =
        underTest.gaugeBuilder("gauge").buildWithCallback(doubleMeasurementConsumer);

    assertThat(callbacks).isNotEmpty();
    verifyNoInteractions(doubleMeasurementConsumer);

    callbackRegistrar.run();
    verify(doubleMeasurementConsumer, times(1)).accept(any());
    callbackRegistrar.run();
    verify(doubleMeasurementConsumer, times(2)).accept(any());
    callbackRegistrar.run();
    verify(doubleMeasurementConsumer, times(3)).accept(any());

    counter.close();
    assertThat(callbacks).isEmpty();
  }

  @Test
  void createsHistogramMeter() {
    DoubleHistogram histogram = underTest.histogramBuilder("histogram").build();

    assertThat(histogram).isInstanceOf(MicrometerDoubleHistogram.class);
  }
}
