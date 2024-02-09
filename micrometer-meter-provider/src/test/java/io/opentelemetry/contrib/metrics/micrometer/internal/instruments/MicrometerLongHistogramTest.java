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
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongHistogramBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.metrics.micrometer.TestCallbackRegistrar;
import io.opentelemetry.contrib.metrics.micrometer.internal.Constants;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterProviderSharedState;
import io.opentelemetry.contrib.metrics.micrometer.internal.state.MeterSharedState;
import io.opentelemetry.extension.incubator.metrics.ExtendedLongHistogramBuilder;
import java.util.Arrays;
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
    for (long value : RandomUtils.randomLongs(10, 0L, 10L)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value);
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
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
    for (long value : RandomUtils.randomLongs(10, 0L, 10L)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value, attributes);
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
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
    for (long value : RandomUtils.randomLongs(10, 0L, 10L)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value, attributes, Context.root());
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
  }

  @Test
  void addWithAttributesAndAdvice() {
    LongHistogramBuilder builder =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit");

    ((ExtendedLongHistogramBuilder) builder)
        .setAttributesAdvice(
            Arrays.asList(
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_NAME),
                AttributeKey.stringKey(Constants.OTEL_INSTRUMENTATION_VERSION),
                AttributeKey.stringKey("key")));

    LongHistogram underTest = builder.build();

    assertThat(meterRegistry.getMeters()).isEmpty();

    Attributes attributes =
        Attributes.builder().put("key", "value").put("unwanted", "value").build();
    underTest.record(10, attributes);

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
    for (long value : RandomUtils.randomLongs(10, 0L, 10L)) {
      expectedCount += 1;
      expectedTotal += value;

      underTest.record(value, attributes);
      assertThat(summary.count()).isEqualTo(expectedCount);
      assertThat(summary.totalAmount()).isEqualTo(expectedTotal);
    }
  }

  @Test
  void addWithExplicitBucketBoundaries() {
    LongHistogram underTest =
        MicrometerDoubleHistogram.builder(meterSharedState, "histogram")
            .ofLongs()
            .setDescription("description")
            .setUnit("unit")
            .setExplicitBucketBoundariesAdvice(Arrays.asList(10L, 20L, 30L))
            .build();

    underTest.record(5L);
    underTest.record(15L);
    underTest.record(25L);
    underTest.record(35L);

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
