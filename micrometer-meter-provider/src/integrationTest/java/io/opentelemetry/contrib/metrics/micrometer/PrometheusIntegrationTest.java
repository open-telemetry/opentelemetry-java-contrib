/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleCounterBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleGaugeBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleUpDownCounterBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedLongCounterBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedLongGaugeBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedLongHistogramBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedLongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleCounterBuilder;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.DoubleUpDownCounterBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongHistogramBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PrometheusIntegrationTest {
  private static final AttributeKey<String> KEY1 = AttributeKey.stringKey("key1");
  private static final AttributeKey<String> KEY2 = AttributeKey.stringKey("key2");
  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";

  private static final Attributes FIRST_ATTRIBUTES = Attributes.of(KEY1, VALUE1, KEY2, VALUE1);
  private static final Attributes SECOND_ATTRIBUTES = Attributes.of(KEY1, VALUE1, KEY2, VALUE2);

  PrometheusMeterRegistry prometheusMeterRegistry;

  Meter meter;

  @BeforeEach
  void setUp() {
    prometheusMeterRegistry = new PrometheusMeterRegistry(DefaultPrometheusConfig.INSTANCE);
    MeterProvider meterProvider = MicrometerMeterProvider.builder(prometheusMeterRegistry).build();
    meter = meterProvider.meterBuilder("integrationTest").setInstrumentationVersion("1.0").build();
  }

  @Test
  void noMeters() {
    String output = prometheusMeterRegistry.scrape();

    assertThat(output).isEmpty();
  }

  @Test
  void longCounter() {
    LongCounter longCounter =
        meter
            .counterBuilder("longCounter")
            .setDescription("LongCounter test")
            .setUnit("units")
            .build();

    longCounter.add(1, FIRST_ATTRIBUTES);
    longCounter.add(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP longCounter_units_total LongCounter test")
        .contains("# TYPE longCounter_units_total counter")
        .contains(
            "longCounter_units_total{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.0")
        .contains(
            "longCounter_units_total{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.0");
  }

  @Test
  void longCounterWithAttributesAdvice() {
    LongCounterBuilder builder =
        meter.counterBuilder("longCounter").setDescription("LongCounter test").setUnit("units");

    ((ExtendedLongCounterBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    LongCounter longCounter = builder.build();

    longCounter.add(1, FIRST_ATTRIBUTES);
    longCounter.add(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP longCounter_units_total LongCounter test")
        .contains("# TYPE longCounter_units_total counter")
        .contains(
            "longCounter_units_total{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 3.0")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");
  }

  @Test
  void observableLongCounter() {
    try (ObservableLongCounter observableLongCounter =
        meter
            .counterBuilder("longCounter")
            .setDescription("LongCounter test")
            .setUnit("units")
            .buildWithCallback(
                onlyOnce(
                    measurement -> {
                      measurement.record(1, FIRST_ATTRIBUTES);
                      measurement.record(2, SECOND_ATTRIBUTES);
                    }))) {

      String output = scrapeFor("longCounter");
      assertThat(output)
          .contains("# TYPE longCounter_units_total counter")
          .contains("# HELP longCounter_units_total LongCounter test")
          .contains(
              "longCounter_units_total{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
          .contains(
              "longCounter_units_total{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2");
    }
  }

  @Test
  void observableLongCounterWithAttributesAdvice() {
    LongCounterBuilder builder =
        meter.counterBuilder("longCounter").setDescription("LongCounter test").setUnit("units");

    ((ExtendedLongCounterBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    try (ObservableLongCounter observableLongCounter =
        builder.buildWithCallback(
            onlyOnce(
                measurement -> {
                  measurement.record(1, FIRST_ATTRIBUTES);
                  measurement.record(2, SECOND_ATTRIBUTES);
                }))) {

      String output = scrapeFor("longCounter");
      assertThat(output)
          .contains("# TYPE longCounter_units_total counter")
          .contains("# HELP longCounter_units_total LongCounter test")
          .contains(
              "longCounter_units_total{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
          .doesNotContain("key2=\"value1\"")
          .doesNotContain("key2=\"value2\"");
    }
  }

  @Test
  void doubleCounter() {
    DoubleCounter doubleCounter =
        meter
            .counterBuilder("doubleCounter")
            .ofDoubles()
            .setDescription("DoubleCounter test")
            .setUnit("units")
            .build();

    doubleCounter.add(1.5, FIRST_ATTRIBUTES);
    doubleCounter.add(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP doubleCounter_units_total DoubleCounter test")
        .contains("# TYPE doubleCounter_units_total counter")
        .contains(
            "doubleCounter_units_total{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
        .contains(
            "doubleCounter_units_total{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");
  }

  @Test
  void doubleCounterWithAttributesAdvice() {

    DoubleCounterBuilder builder =
        meter
            .counterBuilder("doubleCounter")
            .ofDoubles()
            .setDescription("DoubleCounter test")
            .setUnit("units");

    ((ExtendedDoubleCounterBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    DoubleCounter doubleCounter = builder.build();

    doubleCounter.add(1.5, FIRST_ATTRIBUTES);
    doubleCounter.add(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP doubleCounter_units_total DoubleCounter test")
        .contains("# TYPE doubleCounter_units_total counter")
        .contains(
            "doubleCounter_units_total{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 4.0")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");
  }

  @Test
  void observableDoubleCounter() {
    try (ObservableDoubleCounter observableDoubleCounter =
        meter
            .counterBuilder("doubleCounter")
            .ofDoubles()
            .setDescription("DoubleCounter test")
            .setUnit("units")
            .buildWithCallback(
                onlyOnce(
                    measurement -> {
                      measurement.record(1.5, FIRST_ATTRIBUTES);
                      measurement.record(2.5, SECOND_ATTRIBUTES);
                    }))) {

      String output = scrapeFor("doubleCounter");
      assertThat(output)
          .contains("# TYPE doubleCounter_units_total counter")
          .contains("# HELP doubleCounter_units_total DoubleCounter test")
          .contains(
              "doubleCounter_units_total{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
          .contains(
              "doubleCounter_units_total{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");
    }
  }

  @Test
  void observableDoubleCounterWithAttributesAdvice() {
    DoubleCounterBuilder builder =
        meter
            .counterBuilder("doubleCounter")
            .ofDoubles()
            .setDescription("DoubleCounter test")
            .setUnit("units");

    ((ExtendedDoubleCounterBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    try (ObservableDoubleCounter observableDoubleCounter =
        builder.buildWithCallback(
            onlyOnce(
                measurement -> {
                  measurement.record(1.0, FIRST_ATTRIBUTES);
                  measurement.record(2.0, SECOND_ATTRIBUTES);
                }))) {

      String output = scrapeFor("doubleCounter");
      assertThat(output)
          .contains("# TYPE doubleCounter_units_total counter")
          .contains("# HELP doubleCounter_units_total DoubleCounter test")
          .contains(
              "doubleCounter_units_total{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
          .doesNotContain("key2=\"value1\"")
          .doesNotContain("key2=\"value2\"");
    }
  }

  @Test
  void longUpDownCounter() {
    LongUpDownCounter longUpDownCounter =
        meter
            .upDownCounterBuilder("longUpDownCounter")
            .setDescription("LongUpDownCounter test")
            .setUnit("units")
            .build();

    longUpDownCounter.add(1, FIRST_ATTRIBUTES);
    longUpDownCounter.add(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP longUpDownCounter_units LongUpDownCounter test")
        .contains("# TYPE longUpDownCounter_units gauge")
        .contains(
            "longUpDownCounter_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.0")
        .contains(
            "longUpDownCounter_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.0");

    longUpDownCounter.add(1, FIRST_ATTRIBUTES);
    longUpDownCounter.add(2, SECOND_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains(
            "longUpDownCounter_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.0")
        .contains(
            "longUpDownCounter_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 4.0");
  }

  @Test
  void longUpDownCounterWithAttributesAdvice() {

    LongUpDownCounterBuilder builder =
        meter
            .upDownCounterBuilder("longUpDownCounter")
            .setDescription("LongUpDownCounter test")
            .setUnit("units");

    ((ExtendedLongUpDownCounterBuilder) builder)
        .setAttributesAdvice(Collections.singletonList(KEY1));

    LongUpDownCounter longUpDownCounter = builder.build();

    longUpDownCounter.add(1, FIRST_ATTRIBUTES);
    longUpDownCounter.add(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP longUpDownCounter_units LongUpDownCounter test")
        .contains("# TYPE longUpDownCounter_units gauge")
        .contains(
            "longUpDownCounter_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 3.0")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");

    longUpDownCounter.add(1, FIRST_ATTRIBUTES);
    longUpDownCounter.add(2, SECOND_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains(
            "longUpDownCounter_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 6.0")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");
  }

  @Test
  void observableLongUpDownCounter() {
    try (ObservableLongUpDownCounter observableLongUpDownCounter =
        meter
            .upDownCounterBuilder("longUpDownCounter")
            .setDescription("LongUpDownCounter test")
            .setUnit("units")
            .buildWithCallback(
                measurement -> {
                  measurement.record(1, FIRST_ATTRIBUTES);
                  measurement.record(-2, SECOND_ATTRIBUTES);
                })) {

      String output = scrapeFor("longUpDownCounter");
      assertThat(output)
          .contains("# TYPE longUpDownCounter_units gauge")
          .contains("# HELP longUpDownCounter_units LongUpDownCounter test")
          .contains(
              "longUpDownCounter_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.0")
          .contains(
              "longUpDownCounter_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} -2.0");
    }
  }

  @Test
  void observableLongUpDownCounterWithAttributesAdvice() {
    LongUpDownCounterBuilder builder =
        meter
            .upDownCounterBuilder("longUpDownCounter")
            .setDescription("LongUpDownCounter test")
            .setUnit("units");

    ((ExtendedLongUpDownCounterBuilder) builder)
        .setAttributesAdvice(Collections.singletonList(KEY1));

    try (ObservableLongUpDownCounter observableLongUpDownCounter =
        builder.buildWithCallback(
            measurement -> {
              measurement.record(1, FIRST_ATTRIBUTES);
              measurement.record(-2, SECOND_ATTRIBUTES);
            })) {

      String output = scrapeFor("longUpDownCounter");
      assertThat(output)
          .contains("# TYPE longUpDownCounter_units gauge")
          .contains("# HELP longUpDownCounter_units LongUpDownCounter test")
          .contains(
              "longUpDownCounter_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} -2.0")
          .doesNotContain("key2=\"value1\"")
          .doesNotContain("key2=\"value2\"");
    }
  }

  @Test
  void doubleUpDownCounter() {
    DoubleUpDownCounter doubleUpDownCounter =
        meter
            .upDownCounterBuilder("doubleUpDownCounter")
            .ofDoubles()
            .setDescription("DoubleUpDownCounter test")
            .setUnit("units")
            .build();

    doubleUpDownCounter.add(1.5, FIRST_ATTRIBUTES);
    doubleUpDownCounter.add(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP doubleUpDownCounter_units DoubleUpDownCounter test")
        .contains("# TYPE doubleUpDownCounter_units gauge")
        .contains(
            "doubleUpDownCounter_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
        .contains(
            "doubleUpDownCounter_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");

    doubleUpDownCounter.add(0.5, FIRST_ATTRIBUTES);
    doubleUpDownCounter.add(-1.5, SECOND_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains(
            "doubleUpDownCounter_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.0")
        .contains(
            "doubleUpDownCounter_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.0");
  }

  @Test
  void doubleUpDownCounterWithAttributesAdvice() {
    DoubleUpDownCounterBuilder builder =
        meter
            .upDownCounterBuilder("doubleUpDownCounter")
            .ofDoubles()
            .setDescription("DoubleUpDownCounter test")
            .setUnit("units");

    ((ExtendedDoubleUpDownCounterBuilder) builder)
        .setAttributesAdvice(Collections.singletonList(KEY1));

    DoubleUpDownCounter doubleUpDownCounter = builder.build();

    doubleUpDownCounter.add(1.5, FIRST_ATTRIBUTES);
    doubleUpDownCounter.add(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP doubleUpDownCounter_units DoubleUpDownCounter test")
        .contains("# TYPE doubleUpDownCounter_units gauge")
        .contains(
            "doubleUpDownCounter_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 4.0")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");
  }

  @Test
  void observableDoubleUpDownCounter() {
    try (ObservableDoubleUpDownCounter observableDoubleUpDownCounter =
        meter
            .upDownCounterBuilder("doubleUpDownCounter")
            .ofDoubles()
            .setDescription("DoubleUpDownCounter test")
            .setUnit("units")
            .buildWithCallback(
                onlyOnce(
                    measurement -> {
                      measurement.record(1.5, FIRST_ATTRIBUTES);
                      measurement.record(-2.5, SECOND_ATTRIBUTES);
                    }))) {

      String output = scrapeFor("doubleUpDownCounter");
      assertThat(output)
          .contains("# TYPE doubleUpDownCounter_units gauge")
          .contains("# HELP doubleUpDownCounter_units DoubleUpDownCounter test")
          .contains(
              "doubleUpDownCounter_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
          .contains(
              "doubleUpDownCounter_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} -2.5");
    }
  }

  @Test
  void observableDoubleUpDownCounterWithAttributesAdvice() {
    DoubleUpDownCounterBuilder builder =
        meter
            .upDownCounterBuilder("doubleUpDownCounter")
            .ofDoubles()
            .setDescription("DoubleUpDownCounter test")
            .setUnit("units");

    ((ExtendedDoubleUpDownCounterBuilder) builder)
        .setAttributesAdvice(Collections.singletonList(KEY1));

    try (ObservableDoubleUpDownCounter observableDoubleUpDownCounter =
        builder.buildWithCallback(
            onlyOnce(
                measurement -> {
                  measurement.record(1.5, FIRST_ATTRIBUTES);
                  measurement.record(-2.5, SECOND_ATTRIBUTES);
                }))) {

      String output = scrapeFor("doubleUpDownCounter");
      assertThat(output)
          .contains("# TYPE doubleUpDownCounter_units gauge")
          .contains("# HELP doubleUpDownCounter_units DoubleUpDownCounter test")
          .contains(
              "doubleUpDownCounter_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} -2.5")
          .doesNotContain("key2=\"value1\"")
          .doesNotContain("key2=\"value2\"");
    }
  }

  @Test
  void doubleHistogram() {
    DoubleHistogram doubleHistogram =
        meter
            .histogramBuilder("doubleHistogram")
            .setDescription("DoubleHistogram test")
            .setUnit("units")
            .build();

    doubleHistogram.record(1.5, FIRST_ATTRIBUTES);
    doubleHistogram.record(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP doubleHistogram_units DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units summary")
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5")
        .contains("# HELP doubleHistogram_units_max DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units_max gauge")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");

    doubleHistogram.record(2.5, FIRST_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 4")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");
  }

  @Test
  void doubleHistogramWithAttributesAdvice() {

    DoubleHistogramBuilder builder =
        meter
            .histogramBuilder("doubleHistogram")
            .setDescription("DoubleHistogram test")
            .setUnit("units");

    ((ExtendedDoubleHistogramBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    DoubleHistogram doubleHistogram = builder.build();

    doubleHistogram.record(1.5, FIRST_ATTRIBUTES);
    doubleHistogram.record(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP doubleHistogram_units DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units summary")
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 4")
        .contains("# HELP doubleHistogram_units_max DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units_max gauge")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");
  }

  @Test
  void doubleHistogramWithExplicitBucketBoundaries() {
    DoubleHistogram doubleHistogram =
        meter
            .histogramBuilder("doubleHistogram")
            .setExplicitBucketBoundariesAdvice(Arrays.asList(1.0, 2.0, 3.0))
            .setDescription("DoubleHistogram test")
            .setUnit("units")
            .build();

    doubleHistogram.record(1.5, FIRST_ATTRIBUTES);
    doubleHistogram.record(2.5, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP doubleHistogram_units DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units histogram")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"1.0\"} 0")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"2.0\"} 1")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"3.0\"} 1")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"+Inf\"} 1")
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"1.0\"} 0")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"2.0\"} 0")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"3.0\"} 1")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"+Inf\"} 1")
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5")
        .contains("# HELP doubleHistogram_units_max DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units_max gauge")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");

    doubleHistogram.record(2.5, FIRST_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"1.0\"} 0")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"2.0\"} 1")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"3.0\"} 2")
        .contains(
            "doubleHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"+Inf\"} 2")
        .contains(
            "doubleHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains(
            "doubleHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 4")
        .contains(
            "doubleHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");
  }

  @Test
  void longHistogram() {
    LongHistogram longHistogram =
        meter
            .histogramBuilder("longHistogram")
            .ofLongs()
            .setDescription("LongHistogram test")
            .setUnit("units")
            .build();

    longHistogram.record(1, FIRST_ATTRIBUTES);
    longHistogram.record(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP longHistogram_units LongHistogram test")
        .contains("# TYPE longHistogram_units summary")
        .contains(
            "longHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_count{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains("# HELP longHistogram_units_max LongHistogram test")
        .contains("# TYPE longHistogram_units_max gauge")
        .contains(
            "longHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_max{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2");

    longHistogram.record(2, FIRST_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains(
            "longHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 3")
        .contains(
            "longHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2");
  }

  @Test
  void longHistogramWithAttributesAdvice() {
    LongHistogramBuilder builder =
        meter
            .histogramBuilder("longHistogram")
            .ofLongs()
            .setDescription("LongHistogram test")
            .setUnit("units");

    ((ExtendedLongHistogramBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    LongHistogram longHistogram = builder.build();

    longHistogram.record(1, FIRST_ATTRIBUTES);
    longHistogram.record(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP longHistogram_units LongHistogram test")
        .contains("# TYPE longHistogram_units summary")
        .contains(
            "longHistogram_units_count{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 3")
        .contains("# HELP longHistogram_units_max LongHistogram test")
        .contains("# TYPE longHistogram_units_max gauge")
        .contains(
            "longHistogram_units_max{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .doesNotContain("key2=\"value1\"")
        .doesNotContain("key2=\"value2\"");
  }

  @Test
  void longHistogramWithExplicitBucketBoundaries() {
    LongHistogram longHistogram =
        meter
            .histogramBuilder("longHistogram")
            .ofLongs()
            .setExplicitBucketBoundariesAdvice(Arrays.asList(1L, 2L, 3L))
            .setDescription("LongHistogram test")
            .setUnit("units")
            .build();

    longHistogram.record(1, FIRST_ATTRIBUTES);
    longHistogram.record(2, SECOND_ATTRIBUTES);

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP longHistogram_units LongHistogram test")
        .contains("# TYPE longHistogram_units histogram")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"1.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"2.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"3.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"+Inf\"} 1")
        .contains(
            "longHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"1.0\"} 0")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"2.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"3.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"+Inf\"} 1")
        .contains(
            "longHistogram_units_count{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains("# HELP longHistogram_units_max LongHistogram test")
        .contains("# TYPE longHistogram_units_max gauge")
        .contains(
            "longHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1")
        .contains(
            "longHistogram_units_max{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2");

    longHistogram.record(2, FIRST_ATTRIBUTES);

    output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"1.0\"} 1")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"2.0\"} 2")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"3.0\"} 2")
        .contains(
            "longHistogram_units_bucket{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\",le=\"+Inf\"} 2")
        .contains(
            "longHistogram_units_count{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2")
        .contains(
            "longHistogram_units_sum{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 3")
        .contains(
            "longHistogram_units_max{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2");
  }

  @Test
  void observableDoubleGauge() {
    try (ObservableDoubleGauge observableDoubleGauge =
        meter
            .gaugeBuilder("doubleGauge")
            .setDescription("DoubleGauge test")
            .setUnit("units")
            .buildWithCallback(
                measurement -> {
                  measurement.record(1.5, FIRST_ATTRIBUTES);
                  measurement.record(2.5, SECOND_ATTRIBUTES);
                })) {

      String output = scrapeFor("doubleGauge");
      assertThat(output)
          .contains("# TYPE doubleGauge_units gauge")
          .contains("# HELP doubleGauge_units DoubleGauge test")
          .contains(
              "doubleGauge_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.5")
          .contains(
              "doubleGauge_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5");
    }
  }

  @Test
  void observableDoubleGaugeWithAttributesAdvice() {
    DoubleGaugeBuilder builder =
        meter.gaugeBuilder("doubleGauge").setDescription("DoubleGauge test").setUnit("units");

    ((ExtendedDoubleGaugeBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    try (ObservableDoubleGauge observableDoubleGauge =
        builder.buildWithCallback(
            measurement -> {
              measurement.record(1.5, FIRST_ATTRIBUTES);
              measurement.record(2.5, SECOND_ATTRIBUTES);
            })) {

      String output = scrapeFor("doubleGauge");
      assertThat(output)
          .contains("# TYPE doubleGauge_units gauge")
          .contains("# HELP doubleGauge_units DoubleGauge test")
          .contains(
              "doubleGauge_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.5")
          .doesNotContain("key2=\"value1\"")
          .doesNotContain("key2=\"value2\"");
    }
  }

  @Test
  void observableLongGauge() {
    try (ObservableLongGauge observableLongGauge =
        meter
            .gaugeBuilder("longGauge")
            .ofLongs()
            .setDescription("LongGauge test")
            .setUnit("units")
            .buildWithCallback(
                measurement -> {
                  measurement.record(1, FIRST_ATTRIBUTES);
                  measurement.record(2, SECOND_ATTRIBUTES);
                })) {

      String output = scrapeFor("longGauge");
      assertThat(output)
          .contains("# TYPE longGauge_units gauge")
          .contains("# HELP longGauge_units LongGauge test")
          .contains(
              "longGauge_units{key1=\"value1\",key2=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 1.0")
          .contains(
              "longGauge_units{key1=\"value1\",key2=\"value2\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.0");
    }
  }

  @Test
  void observableLongGaugeWithAttributesAdvice() {
    LongGaugeBuilder builder =
        meter.gaugeBuilder("longGauge").ofLongs().setDescription("LongGauge test").setUnit("units");

    ((ExtendedLongGaugeBuilder) builder).setAttributesAdvice(Collections.singletonList(KEY1));

    try (ObservableLongGauge observableLongGauge =
        builder.buildWithCallback(
            measurement -> {
              measurement.record(1, FIRST_ATTRIBUTES);
              measurement.record(2, SECOND_ATTRIBUTES);
            })) {

      String output = scrapeFor("longGauge");
      assertThat(output)
          .contains("# TYPE longGauge_units gauge")
          .contains("# HELP longGauge_units LongGauge test")
          .contains(
              "longGauge_units{key1=\"value1\",otel_instrumentation_name=\"integrationTest\",otel_instrumentation_version=\"1.0\"} 2.0")
          .doesNotContain("key2=\"value1\"")
          .doesNotContain("key2=\"value2\"");
    }
  }

  private String scrapeFor(String value) {
    String output = prometheusMeterRegistry.scrape();
    if (!output.contains(value)) {
      output = prometheusMeterRegistry.scrape();
    }
    assertThat(output).contains(value);
    return output;
  }

  private <T> Consumer<T> onlyOnce(Consumer<T> consumer) {
    return new Consumer<T>() {
      final AtomicBoolean first = new AtomicBoolean(true);

      @Override
      public void accept(T t) {
        if (first.compareAndSet(true, false)) {
          consumer.accept(t);
        }
      }
    };
  }

  enum DefaultPrometheusConfig implements PrometheusConfig {
    INSTANCE;

    @Override
    public String get(String key) {
      return null;
    }
  }
}
