/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerDoubleGaugeTest {
  SimpleMeterRegistry meterRegistry;

  List<Runnable> callbacks;

  TestCallbackRegistrar callbackRegistrar;

  MeterProviderSharedState meterProviderSharedState;

  MeterSharedState meterSharedState;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new ArrayList<>();
    callbackRegistrar = new TestCallbackRegistrar(callbacks);
    meterProviderSharedState = new MeterProviderSharedState(() -> meterRegistry, callbackRegistrar);
    meterSharedState = new MeterSharedState(meterProviderSharedState, "meter", "1.0", null);
  }

  @Test
  void observable() {
    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();

    ObservableDoubleGauge underTest =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicDoubleCounter.current()));

    assertThat(callbacks).hasSize(1);

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    Gauge gauge = meterRegistry.find("gauge").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("gauge");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    for (double value : RandomUtils.randomDoubles(10, 0.0, 500.0)) {
      atomicDoubleCounter.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo(value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableDoubleGauge underTest =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(
                measurement -> measurement.record(atomicDoubleCounter.current(), attributes));

    assertThat(callbacks).hasSize(1);

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    Gauge gauge = meterRegistry.find("gauge").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("gauge");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    for (double value : RandomUtils.randomDoubles(10, 0.0, 500.0)) {
      atomicDoubleCounter.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo(value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
