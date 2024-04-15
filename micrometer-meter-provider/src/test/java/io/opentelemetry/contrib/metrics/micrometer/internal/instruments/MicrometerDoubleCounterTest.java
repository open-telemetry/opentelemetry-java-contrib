/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleCounterBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleCounterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerDoubleCounterTest {

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
    DoubleCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.add(10.0);

    Counter counter = meterRegistry.find("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    underTest.add(10.0);
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    underTest.add(-5.0);
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 20.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += value;

      underTest.add(value);
      assertThat(counter.count()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributes() {
    DoubleCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10.0, attributes);

    Counter counter = meterRegistry.find("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    underTest.add(10.0, attributes);
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    underTest.add(-5.0, attributes);
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 20.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += value;

      underTest.add(value, attributes);
      assertThat(counter.count()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributesAndContext() {
    DoubleCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10.0, attributes, Context.root());

    Counter counter = meterRegistry.find("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    underTest.add(10.0, attributes, Context.root());
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    underTest.add(-5.0, attributes, Context.root());
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 20.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += value;

      underTest.add(value, attributes, Context.root());
      assertThat(counter.count()).isEqualTo(expectedCount);
    }
  }

  @Test
  void addWithAttributesAndAttributesAdvice() {
    DoubleCounterBuilder builder =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedDoubleCounterBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    DoubleCounter underTest = builder.build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    underTest.add(10.0, attributes);

    Counter counter = meterRegistry.find("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    underTest.add(10.0, attributes);
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    underTest.add(-5.0, attributes);
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 20.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += value;

      underTest.add(value, attributes);
      assertThat(counter.count()).isEqualTo(expectedCount);
    }
  }

  @Test
  void observable() {
    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();
    ObservableDoubleCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicDoubleCounter.current()));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    FunctionCounter counter = meterRegistry.find("counter").functionCounter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    atomicDoubleCounter.set(20.0);
    callbackRegistrar.run();
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    atomicDoubleCounter.set(5.0);
    callbackRegistrar.run();
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 20.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 500.0)) {
      expectedCount += value;

      atomicDoubleCounter.set(expectedCount);
      callbackRegistrar.run();
      assertThat(counter.count()).isEqualTo(expectedCount);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableDoubleCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(
                measurement -> measurement.record(atomicDoubleCounter.current(), attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    FunctionCounter counter = meterRegistry.find("counter").functionCounter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    atomicDoubleCounter.set(20.0);
    callbackRegistrar.run();
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    atomicDoubleCounter.set(5.0);
    callbackRegistrar.run();
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 10.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 500.0)) {
      expectedCount += value;

      atomicDoubleCounter.set(expectedCount);
      callbackRegistrar.run();
      assertThat(counter.count()).isEqualTo(expectedCount);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributesAndAttributesAdvice() {
    DoubleCounterBuilder builder =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedDoubleCounterBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    AtomicDoubleCounter atomicDoubleCounter = new AtomicDoubleCounter();
    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    ObservableDoubleCounter underTest =
        builder.buildWithCallback(
            measurement -> measurement.record(atomicDoubleCounter.current(), attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    atomicDoubleCounter.set(10.0);
    callbackRegistrar.run();
    FunctionCounter counter = meterRegistry.find("counter").functionCounter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    // test that counter can be increased
    atomicDoubleCounter.set(20.0);
    callbackRegistrar.run();
    assertThat(counter.count()).isEqualTo(20.0);

    // test that counter cannot be decreased
    atomicDoubleCounter.set(5.0);
    callbackRegistrar.run();
    assertThat(counter.count()).isEqualTo(20.0);

    double expectedCount = 10.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 500.0)) {
      expectedCount += value;

      atomicDoubleCounter.set(expectedCount);
      callbackRegistrar.run();
      assertThat(counter.count()).isEqualTo(expectedCount);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
