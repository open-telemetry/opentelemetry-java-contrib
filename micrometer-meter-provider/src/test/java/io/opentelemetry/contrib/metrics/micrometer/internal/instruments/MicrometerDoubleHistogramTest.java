/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerDoubleHistogramTest {

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
    DoubleHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .setDescription("description")
            .setUnit("unit")
            .build();

    underTest.record(10.0);

    DistributionSummary summary = meterRegistry.find("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    long expectedCount = 1;
    double expectedTotal = 10.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value);
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
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

    DistributionSummary summary = meterRegistry.find("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    long expectedCount = 1;
    double expectedTotal = 10.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value, attributes);
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
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

    DistributionSummary summary = meterRegistry.find("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    long expectedCount = 1;
    double expectedTotal = 10.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value, attributes, Context.root());
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
  }

  @Test
  void addWithAttributesAndAttributesAdvice() {

    DoubleHistogramBuilder builder =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedDoubleHistogramBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    DoubleHistogram underTest = builder.build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    underTest.record(10.0, attributes);

    DistributionSummary summary = meterRegistry.find("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of("key", "value"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(1);
    assertThat(summary.totalAmount()).isEqualTo(10.0);

    long expectedCount = 1;
    double expectedTotal = 10.0;
    for (double value : RandomUtils.randomDoubles(10, 0.0, 10.0)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value, attributes);
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
  }

  @Test
  void addWithExplicitBucketBoundaries() {
    DoubleHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .setDescription("description")
            .setUnit("unit")
            .setExplicitBucketBoundariesAdvice(Arrays.asList(10.0, 20.0, 30.0))
            .build();

    underTest.record(5.0);
    underTest.record(15.0);
    underTest.record(25.0);
    underTest.record(35.0);

    DistributionSummary summary = meterRegistry.find("histogram").summary();
    assertThat(summary).isNotNull();
    Meter.Id id = summary.getId();
    assertThat(id.getName()).isEqualTo("histogram");
    assertThat(id.getTags())
        .containsExactlyInAnyOrder(
            Tag.of(Constants.OTEL_INSTRUMENTATION_NAME, "meter"),
            Tag.of(Constants.OTEL_INSTRUMENTATION_VERSION, "1.0"));
    assertThat(id.getDescription()).isEqualTo("description");
    assertThat(id.getBaseUnit()).isEqualTo("unit");
    assertThat(summary.count()).isEqualTo(4);
    assertThat(summary.totalAmount()).isEqualTo(80.0);

    HistogramSnapshot snapshot = summary.takeSnapshot();
    CountAtBucket[] counts = snapshot.histogramCounts();
    assertThat(counts)
        .hasSize(3)
        .containsExactly(
            new CountAtBucket(10.0, 1), new CountAtBucket(20.0, 2), new CountAtBucket(30.0, 3));
  }
}
