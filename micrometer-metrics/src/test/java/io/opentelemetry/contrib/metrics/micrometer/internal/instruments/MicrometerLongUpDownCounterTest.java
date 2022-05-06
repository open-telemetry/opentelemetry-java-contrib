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
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerLongUpDownCounterTest {

  SimpleMeterRegistry meterRegistry;

  List<Runnable> callbacks;

  MeterProviderSharedState meterProviderSharedState;

  MeterSharedState meterSharedState;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new ArrayList<>();
    meterProviderSharedState = new MeterProviderSharedState(meterRegistry, callbacks);
    meterSharedState = new MeterSharedState(meterProviderSharedState, "meter", null, null);
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
    assertThat(id.getTags()).isEmpty();
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10);
    assertThat(gauge.value()).isEqualTo(0.0);
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
    assertThat(id.getTags()).isEqualTo(Collections.singletonList(Tag.of("key", "value")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10, attributes);
    assertThat(gauge.value()).isEqualTo(0.0);
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
    assertThat(id.getTags()).isEqualTo(Collections.singletonList(Tag.of("key", "value")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.add(-10, attributes, Context.root());
    assertThat(gauge.value()).isEqualTo(0.0);
  }

  @Test
  void observable() {
    ObservableLongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10));

    assertThat(callbacks).hasSize(1);
    Runnable callback = callbacks.get(0);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callback.run();
    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags()).isEmpty();
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    callback.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableLongUpDownCounter underTest =
        MicrometerLongUpDownCounter.builder(meterSharedState, "upDownCounter")
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10, attributes));

    assertThat(callbacks).hasSize(1);
    Runnable callback = callbacks.get(0);

    assertThat(meterRegistry.getMeters()).isEmpty();

    callback.run();
    Gauge gauge = meterRegistry.find("upDownCounter").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("upDownCounter");
    assertThat(id.getTags()).isEqualTo(Collections.singletonList(Tag.of("key", "value")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    callback.run();
    assertThat(gauge.value()).isEqualTo(20.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
