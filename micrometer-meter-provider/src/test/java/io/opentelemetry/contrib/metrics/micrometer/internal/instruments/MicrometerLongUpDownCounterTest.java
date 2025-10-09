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
import io.opentelemetry.api.incubator.metrics.ExtendedLongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrometerLongUpDownCounterTest {

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

    // test that counter can be increased
    underTest.add(10);
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    underTest.add(-5);
    assertThat(gauge.value()).isEqualTo(15.0);

    double expectedCount = 15.0;
    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
      expectedCount += value;

      underTest.add(value);
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
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

    // test that counter can be increased
    underTest.add(10, attributes);
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    underTest.add(-5, attributes);
    assertThat(gauge.value()).isEqualTo(15.0);

    double expectedCount = 15.0;
    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
      expectedCount += value;

      underTest.add(value, attributes);
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
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

    // test that counter can be increased
    underTest.add(10, attributes, Context.root());
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    underTest.add(-5, attributes, Context.root());
    assertThat(gauge.value()).isEqualTo(15.0);

    double expectedCount = 15.0;
    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
      expectedCount += value;

      underTest.add(value, attributes, Context.root());
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributesAndAttributesAdvice() {
    LongUpDownCounterBuilder builder =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedLongUpDownCounterBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    LongUpDownCounter underTest = builder.build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    underTest.add(10, attributes);

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

    // test that counter can be increased
    underTest.add(10, attributes);
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    underTest.add(-5, attributes);
    assertThat(gauge.value()).isEqualTo(15.0);

    double expectedCount = 15.0;
    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
      expectedCount += value;

      underTest.add(value, attributes);
      assertThat(gauge.value()).isEqualTo(expectedCount);
    }
  }

  @Test
  void observable() {
    AtomicLong atomicLong = new AtomicLong();
    ObservableLongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicLong.get()));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicLong.set(10L);
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

    // test that counter can be increased
    atomicLong.set(20L);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    atomicLong.set(15L);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(15.0);

    for (long value : RandomUtils.randomLongs(10, 0L, 500L)) {
      atomicLong.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo((double) value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    AtomicLong atomicLong = new AtomicLong();
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableLongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicLong.get(), attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicLong.set(10L);
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

    // test that counter can be increased
    atomicLong.set(20L);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    atomicLong.set(15L);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(15.0);

    for (long value : RandomUtils.randomLongs(10, 0L, 500L)) {
      atomicLong.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo((double) value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributesAndAttributesAdvice() {
    LongUpDownCounterBuilder builder =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedLongUpDownCounterBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    AtomicLong atomicLong = new AtomicLong();
    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    ObservableLongUpDownCounter underTest =
        builder.buildWithCallback(measurement -> measurement.record(atomicLong.get(), attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicLong.set(10L);
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

    // test that counter can be increased
    atomicLong.set(20L);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    // test that counter can be decreased
    atomicLong.set(15L);
    callbackRegistrar.run();
    assertThat(gauge.value()).isEqualTo(15.0);

    for (long value : RandomUtils.randomLongs(10, 0L, 500L)) {
      atomicLong.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo((double) value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
