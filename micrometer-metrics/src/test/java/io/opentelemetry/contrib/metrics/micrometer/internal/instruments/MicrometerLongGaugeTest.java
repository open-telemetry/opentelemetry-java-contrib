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
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerLongGaugeTest {
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
  void observable() {
    ObservableLongGauge underTest =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10));

    assertThat(callbacks).hasSize(1);
    Runnable callback = callbacks.get(0);

    callback.run();
    Gauge gauge = meterRegistry.find("gauge").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("gauge");
    assertThat(id.getTags()).isEmpty();
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    callback.run();
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void observableWithAttributes() {
    Attributes attributes = Attributes.builder().put("key", "value").build();
    ObservableLongGauge underTest =
        MicrometerDoubleGauge.builder(meterSharedState, "gauge")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .buildWithCallback(measurement -> measurement.record(10, attributes));

    assertThat(callbacks).hasSize(1);
    Runnable callback = callbacks.get(0);

    callback.run();
    Gauge gauge = meterRegistry.find("gauge").gauge();
    assertThat(gauge).isNotNull();
    Meter.Id id = gauge.getId();
    assertThat(id.getName()).isEqualTo("gauge");
    assertThat(id.getTags()).isEqualTo(Collections.singletonList(Tag.of("key", "value")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(gauge.value()).isEqualTo(10.0);

    callback.run();
    assertThat(gauge.value()).isEqualTo(10.0);

    underTest.close();

    assertThat(callbacks).isEmpty();
  }
}
