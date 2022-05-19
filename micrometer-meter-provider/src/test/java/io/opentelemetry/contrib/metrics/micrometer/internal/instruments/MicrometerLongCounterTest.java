/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerLongCounterTest {

  SimpleMeterRegistry meterRegistry;

  TestCallbackRegistrar callbacks;

  MeterProviderSharedState meterProviderSharedState;

  MeterSharedState meterSharedState;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new TestCallbackRegistrar();
    meterProviderSharedState = new MeterProviderSharedState(() -> meterRegistry, callbacks);
    meterSharedState = new MeterSharedState(meterProviderSharedState, "meter", "1.0", null);
  }

  @Test
  void add() {
    LongCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.add(10);

    Counter counter = meterRegistry.get("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .isEqualTo(
            Arrays.asList(
                Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
                Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);
  }

  @Test
  void addWithAttributes() {
    LongCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10, attributes);

    Counter counter = meterRegistry.get("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .isEqualTo(
            Arrays.asList(
                Tag.of("key", "value"),
                Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
                Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);
  }

  @Test
  void addWithAttributesAndContext() {
    LongCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.add(10, attributes, Context.root());

    Counter counter = meterRegistry.get("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .isEqualTo(
            Arrays.asList(
                Tag.of("key", "value"),
                Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
                Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);
  }

  @Test
  void observable() {
    ObservableLongCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callbacks.run();
    Counter counter = meterRegistry.get("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    callbacks.run();
    assertThat(counter.count()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableLongCounter underTest =
        MicrometerLongCounter.builder(meterSharedState, "counter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10, attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callbacks.run();
    Counter counter = meterRegistry.get("counter").counter();
    assertThat(counter).isNotNull();
    Meter.Id id = counter.getId();
    assertThat(id.getName()).isEqualTo("counter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(counter.count()).isEqualTo(10.0);

    callbacks.run();
    assertThat(counter.count()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
