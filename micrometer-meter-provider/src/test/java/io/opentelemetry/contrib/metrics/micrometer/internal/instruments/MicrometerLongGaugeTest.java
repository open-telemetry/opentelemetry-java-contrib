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
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import io.opentelemetry.api.incubator.metrics.ExtendedLongGaugeBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerLongGaugeTest {
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
    AtomicLong atomicLong = new AtomicLong();
    ObservableLongGauge underTest =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicLong.get()));

    assertThat(callbacks).hasSize(1);

    atomicLong.set(10L);
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

    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
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
    ObservableLongGauge underTest =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(atomicLong.get(), attributes));

    assertThat(callbacks).hasSize(1);

    atomicLong.set(10L);
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

    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
      atomicLong.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo((double) value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributesAndAttributesAdvice() {
    LongGaugeBuilder builder =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedLongGaugeBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    AtomicLong atomicLong = new AtomicLong();
    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    ObservableLongGauge underTest =
        builder.buildWithCallback(measurement -> measurement.record(atomicLong.get(), attributes));

    assertThat(callbacks).hasSize(1);

    atomicLong.set(10L);
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

    for (long value : RandomUtils.randomLongs(10, -500L, 500L)) {
      atomicLong.set(value);
      callbackRegistrar.run();
      assertThat(gauge.value()).isEqualTo((double) value);
    }

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
