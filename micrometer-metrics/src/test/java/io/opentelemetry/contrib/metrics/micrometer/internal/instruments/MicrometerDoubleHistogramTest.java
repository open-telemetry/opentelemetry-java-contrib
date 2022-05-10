/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerDoubleHistogramTest {

  SimpleMeterRegistry meterRegistry;

  TestCallbackRegistrar callbacks;

  MeterProviderSharedState meterProviderSharedState;

  MeterSharedState meterSharedState;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbacks = new TestCallbackRegistrar();
    meterProviderSharedState = new MeterProviderSharedState(meterRegistry, callbacks);
    meterSharedState = new MeterSharedState(meterProviderSharedState, "meter", null, null);
  }

  @Test
  void add() {
    DoubleHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.record(10.0);

    DistributionSummary summary = meterRegistry.get("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags()).isEmpty();
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    underTest.record(5.0);
    assertThat(summary.count()).isEqualTo(2);
    assertThat(summary.totalAmount()).isEqualTo(15.0);
  }

  @Test
  void addWithAttributes() {
    DoubleHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.record(10.0, attributes);

    DistributionSummary summary = meterRegistry.get("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags()).isEqualTo(Collections.singletonList(Tag.of("key", "value")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    underTest.record(5.0, attributes);
    assertThat(summary.count()).isEqualTo(2);
    assertThat(summary.totalAmount()).isEqualTo(15.0);
  }

  @Test
  void addWithAttributesAndContext() {
    DoubleHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.record(10.0, attributes, Context.root());

    DistributionSummary summary = meterRegistry.get("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags()).isEqualTo(Collections.singletonList(Tag.of("key", "value")));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    underTest.record(5.0, attributes, Context.root());
    assertThat(summary.count()).isEqualTo(2);
    assertThat(summary.totalAmount()).isEqualTo(15.0);
  }
}
