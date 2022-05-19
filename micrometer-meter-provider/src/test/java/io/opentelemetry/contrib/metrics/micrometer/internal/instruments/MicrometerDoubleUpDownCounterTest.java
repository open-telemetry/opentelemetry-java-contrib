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
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerDoubleUpDownCounterTest {

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
    DoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.add(10.0);

    Gauge gauge = meterRegistry.get("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10.0);
    assertThat(gauge.value()).isEqualTo(0.0);
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

    Gauge gauge = meterRegistry.get("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10.0, attributes);
    assertThat(gauge.value()).isEqualTo(0.0);
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

    Gauge gauge = meterRegistry.get("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10, attributes, Context.root());
    assertThat(gauge.value()).isEqualTo(0.0);
  }

  @Test
  void observable() {
    ObservableDoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10.0));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callbacks.run();
    Gauge gauge = meterRegistry.get("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    callbacks.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableDoubleUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .ofDoubles()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10.0, attributes));

    assertThat(callbacks).hasSize(1);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callbacks.run();
    Gauge gauge = meterRegistry.get("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    callbacks.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
