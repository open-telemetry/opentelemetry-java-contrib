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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.DoubleUpDownCounterBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import io.opentelemetry.extension.incubator.metrics.ExtendedDoubleUpDownCounterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerDoubleUpDownCounterTest {

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
    DoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.add(10.0);

    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10.0);
    assertThat(gauge.value()).isEqualTo(0.0);

    double expectedCount = 0.0;
    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
      expectedCount += value;

      underTest.add(value);
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributes() {
    DoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10.0, attributes);

    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10.0, attributes);
    assertThat(gauge.value()).isEqualTo(0.0);

    double expectedCount = 0.0;
    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
      expectedCount += value;

      underTest.add(value, attributes);
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributesAndContext() {
    DoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10.0, attributes, Context.root());

    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10, attributes, Context.root());
    assertThat(gauge.value()).isEqualTo(0.0);

    double expectedCount = 0.0;
    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
      expectedCount += value;

      underTest.add(value, attributes, Context.root());
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributesAndAdvice() {
    DoubleUpDownCounterBuilder builder =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedDoubleUpDownCounterBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    DoubleUpDownCounter underTest = builder.build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    underTest.add(10.0, attributes);

    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10.0, attributes);
    assertThat(gauge.value()).isEqualTo(0.0);

    double expectedCount = 0.0;
    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
      expectedCount += value;

      underTest.add(value, attributes);
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
  }

  @Test
  void observable() {
    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();
    ObservableDoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicDoubleCounter.current()));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    atomicDoubleCounter.set(0.0);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(0.0);

    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
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
    ObservableDoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(
                measurement -> measurement.record(atomicDoubleCounter.current(), attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    atomicDoubleCounter.set(0.0);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(0.0);

    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
      atomicDoubleCounter.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo(value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributesAndAdvice() {
    DoubleUpDownCounterBuilder builder =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedDoubleUpDownCounterBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();
    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    ObservableDoubleUpDownCounter underTest =
        builder.buildWithCallback(
            measurement -> measurement.record(atomicDoubleCounter.current(), attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    atomicDoubleCounter.set(0.0);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(0.0);

    for (double value : RandomUtils.randomDoubles(10, -500.0, 500.0)) {
      atomicDoubleCounter.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo(value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
