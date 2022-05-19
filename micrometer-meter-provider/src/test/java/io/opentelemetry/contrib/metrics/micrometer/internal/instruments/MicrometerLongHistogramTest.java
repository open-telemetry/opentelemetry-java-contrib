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
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerLongHistogramTest {

  SimpleMeterRegistry meterRegistry;

  TestCallbackRegistrar callbackRegistrar;

  MeterProviderSharedState meterProviderSharedState;

  MeterSharedState meterSharedState;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    callbackRegistrar = new TestCallbackRegistrar(Collections.emptyList());
    meterProviderSharedState = new MeterProviderSharedState(() -> meterRegistry, callbackRegistrar);
    meterSharedState = new MeterSharedState(meterProviderSharedState, "meter", "1.0", null);
  }

  @Test
  void add() {
    LongHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.record(10);

    DistributionSummary summary = meterRegistry.get("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    underTest.record(5);
    assertThat(summary.count()).isEqualTo(2);
    assertThat(summary.totalAmount()).isEqualTo(15.0);
  }

  @Test
  void addWithAttributes() {
    LongHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.record(10, attributes);

    DistributionSummary summary = meterRegistry.get("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    underTest.record(5, attributes);
    assertThat(summary.count()).isEqualTo(2);
    assertThat(summary.totalAmount()).isEqualTo(15.0);
  }

  @Test
  void addWithAttributesAndContext() {
    LongHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes = Attributes.builder().put("key", "value").build();
    underTest.record(10, attributes, Context.root());

    DistributionSummary summary = meterRegistry.get("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    underTest.record(5, attributes, Context.root());
    assertThat(summary.count()).isEqualTo(2);
    assertThat(summary.totalAmount()).isEqualTo(15.0);
  }
}
