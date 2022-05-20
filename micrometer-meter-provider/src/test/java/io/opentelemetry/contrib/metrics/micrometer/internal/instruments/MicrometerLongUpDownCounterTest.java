/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerLongUpDownCounterTest {

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
  void add() {
    LongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.add(10);

    FunctionCounter functionCounter = meterRegistry.find("upDownCounter").functionCounter();
    assertThat(functionCounter).isNotNull();
    Meter.Id id = functionCounter.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(functionCounter.count()).isEqualTo(10.0);

    underTest.add(-10);
    assertThat(functionCounter.count()).isEqualTo(0.0);
  }

  @Test
  void addWithAttributes() {
    LongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10, attributes);

    FunctionCounter functionCounter = meterRegistry.find("upDownCounter").functionCounter();
    assertThat(functionCounter).isNotNull();
    Meter.Id id = functionCounter.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(functionCounter.count()).isEqualTo(10.0);

    underTest.add(-10, attributes);
    assertThat(functionCounter.count()).isEqualTo(0.0);
  }

  @Test
  void addWithAttributesAndContext() {
    LongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10, attributes, Context.root());

    FunctionCounter functionCounter = meterRegistry.find("upDownCounter").functionCounter();
    assertThat(functionCounter).isNotNull();
    Meter.Id id = functionCounter.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(functionCounter.count()).isEqualTo(10.0);

    underTest.add(-10, attributes, Context.root());
    assertThat(functionCounter.count()).isEqualTo(0.0);
  }

  @Test
  void observable() {
    Measureable measureable = new Measureable();
    measureable.value = 10L;
    ObservableLongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(measureable.value));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callbackRegistrar.run();
    FunctionCounter functionCounter = meterRegistry.find("upDownCounter").functionCounter();
    assertThat(functionCounter).isNotNull();
    Meter.Id id = functionCounter.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(functionCounter.count()).isEqualTo(10.0);

    measureable.value = 20L;
    callbackRegistrar.run();
    assertThat(functionCounter.count()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    Measureable measureable = new Measureable();
    measureable.value = 10L;
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableLongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(measureable.value, attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callbackRegistrar.run();
    FunctionCounter functionCounter = meterRegistry.find("upDownCounter").functionCounter();
    assertThat(functionCounter).isNotNull();
    Meter.Id id = functionCounter.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(functionCounter.count()).isEqualTo(10.0);

    measureable.value = 20L;
    callbackRegistrar.run();
    assertThat(functionCounter.count()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  private static class Measureable {
    public long value;
  }
}
