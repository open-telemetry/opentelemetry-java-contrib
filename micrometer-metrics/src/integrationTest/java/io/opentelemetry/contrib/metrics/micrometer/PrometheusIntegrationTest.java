/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PrometheusIntegrationTest {
  private static final AttributeKey<String> KEY1 = AttributeKey.stringKey("key1");

  PrometheusMeterRegistry prometheusMeterRegistry;

  Meter meter;

  @BeforeEach
  void setUp() {
    prometheusMeterRegistry = new PrometheusMeterRegistry(DefaultPrometheusConfig.INSTANCE);
    MeterProvider meterProvider = MicrometerMeterProvider.builder(prometheusMeterRegistry).build();
    meter = meterProvider.get("integrationTest");
  }

  @Test
  void pollingMeter() {
    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP otel_polling_meter")
        .contains("# TYPE otel_polling_meter untyped");
  }

  @Test
  void longCounter() {
    LongCounter longCounter =
        meter
            .counterBuilder("longCounter")
            .setDescription("LongCounter test")
            .setUnit("units")
            .build();

    longCounter.add(1, Attributes.of(KEY1, "value1"));
    longCounter.add(2, Attributes.of(KEY1, "value2"));

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP longCounter_units_total LongCounter test")
        .contains("# TYPE longCounter_units_total counter")
        .contains("longCounter_units_total{key1=\"value1\",} 1.0")
        .contains("longCounter_units_total{key1=\"value2\",} 2.0");
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
                      measurement.record(1, Attributes.of(KEY1, "value1"));
                      measurement.record(2, Attributes.of(KEY1, "value2"));
                    }))) {

      String output = scrapeFor("longCounter");
      assertThat(output)
          .contains("# TYPE longCounter_units_total counter")
          .contains("# HELP longCounter_units_total LongCounter test")
          .contains("longCounter_units_total{key1=\"value1\",} 1.0")
          .contains("longCounter_units_total{key1=\"value2\",} 2.0");
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

    doubleCounter.add(1.5, Attributes.of(KEY1, "value1"));
    doubleCounter.add(2.5, Attributes.of(KEY1, "value2"));

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP doubleCounter_units_total DoubleCounter test")
        .contains("# TYPE doubleCounter_units_total counter")
        .contains("doubleCounter_units_total{key1=\"value2\",} 2.5")
        .contains("doubleCounter_units_total{key1=\"value1\",} 1.5");
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
                      measurement.record(1.5, Attributes.of(KEY1, "value1"));
                      measurement.record(2.5, Attributes.of(KEY1, "value2"));
                    }))) {

      String output = scrapeFor("doubleCounter");
      assertThat(output)
          .contains("# TYPE doubleCounter_units_total counter")
          .contains("# HELP doubleCounter_units_total DoubleCounter test")
          .contains("doubleCounter_units_total{key1=\"value1\",} 1.5")
          .contains("doubleCounter_units_total{key1=\"value2\",} 2.5");
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

    longUpDownCounter.add(1, Attributes.of(KEY1, "value1"));
    longUpDownCounter.add(2, Attributes.of(KEY1, "value2"));

    String output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("# HELP longUpDownCounter_units LongUpDownCounter test")
        .contains("# TYPE longUpDownCounter_units gauge")
        .contains("longUpDownCounter_units{key1=\"value1\",} 1.0")
        .contains("longUpDownCounter_units{key1=\"value2\",} 2.0");

    longUpDownCounter.add(1, Attributes.of(KEY1, "value1"));
    longUpDownCounter.add(2, Attributes.of(KEY1, "value2"));

    output = prometheusMeterRegistry.scrape();

    assertThat(output)
        .contains("longUpDownCounter_units{key1=\"value1\",} 2.0")
        .contains("longUpDownCounter_units{key1=\"value2\",} 4.0");
  }

  @Test
  void observableLongUpDownCounter() {
    try (ObservableLongUpDownCounter observableLongUpDownCounter =
        meter
            .upDownCounterBuilder("longUpDownCounter")
            .setDescription("LongUpDownCounter test")
            .setUnit("units")
            .buildWithCallback(
                onlyOnce(
                    measurement -> {
                      measurement.record(1, Attributes.of(KEY1, "value1"));
                      measurement.record(-2, Attributes.of(KEY1, "value2"));
                    }))) {

      String output = scrapeFor("longUpDownCounter");
      assertThat(output)
          .contains("# TYPE longUpDownCounter_units gauge")
          .contains("# HELP longUpDownCounter_units LongUpDownCounter test")
          .contains("longUpDownCounter_units{key1=\"value1\",} 1.0")
          .contains("longUpDownCounter_units{key1=\"value2\",} -2.0");
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

    doubleUpDownCounter.add(1.5, Attributes.of(KEY1, "value1"));
    doubleUpDownCounter.add(2.5, Attributes.of(KEY1, "value2"));

    String output = prometheusMeterRegistry.scrape();

    assertThat(output).contains("# HELP doubleUpDownCounter_units DoubleUpDownCounter test");
    assertThat(output).contains("# TYPE doubleUpDownCounter_units gauge");
    assertThat(output).contains("doubleUpDownCounter_units{key1=\"value1\",} 1.5");
    assertThat(output).contains("doubleUpDownCounter_units{key1=\"value2\",} 2.5");

    doubleUpDownCounter.add(0.5, Attributes.of(KEY1, "value1"));
    doubleUpDownCounter.add(-1.5, Attributes.of(KEY1, "value2"));

    output = prometheusMeterRegistry.scrape();

    assertThat(output).contains("doubleUpDownCounter_units{key1=\"value1\",} 2.0");
    assertThat(output).contains("doubleUpDownCounter_units{key1=\"value2\",} 1.0");
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
                      measurement.record(1.5, Attributes.of(KEY1, "value1"));
                      measurement.record(-2.5, Attributes.of(KEY1, "value2"));
                    }))) {

      String output = scrapeFor("doubleUpDownCounter");
      assertThat(output)
          .contains("# TYPE doubleUpDownCounter_units gauge")
          .contains("# HELP doubleUpDownCounter_units DoubleUpDownCounter test")
          .contains("doubleUpDownCounter_units{key1=\"value1\",} 1.5")
          .contains("doubleUpDownCounter_units{key1=\"value2\",} -2.5");
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

    doubleHistogram.record(1.5, Attributes.of(KEY1, "value1"));
    doubleHistogram.record(2.5, Attributes.of(KEY1, "value2"));

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP doubleHistogram_units DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units summary")
        .contains("doubleHistogram_units_count{key1=\"value1\",} 1.0")
        .contains("doubleHistogram_units_sum{key1=\"value1\",} 1.5")
        .contains("doubleHistogram_units_count{key1=\"value2\",} 1.0")
        .contains("doubleHistogram_units_sum{key1=\"value2\",} 2.5")
        .contains("# HELP doubleHistogram_units_max DoubleHistogram test")
        .contains("# TYPE doubleHistogram_units_max gauge")
        .contains("doubleHistogram_units_max{key1=\"value1\",} 1.5")
        .contains("doubleHistogram_units_max{key1=\"value2\",} 2.5");

    doubleHistogram.record(2.5, Attributes.of(KEY1, "value1"));

    output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("doubleHistogram_units_count{key1=\"value1\",} 2.0")
        .contains("doubleHistogram_units_sum{key1=\"value1\",} 4.0")
        .contains("doubleHistogram_units_max{key1=\"value1\",} 2.5");
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

    longHistogram.record(1, Attributes.of(KEY1, "value1"));
    longHistogram.record(2, Attributes.of(KEY1, "value2"));

    String output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("# HELP longHistogram_units LongHistogram test")
        .contains("# TYPE longHistogram_units summary")
        .contains("longHistogram_units_count{key1=\"value1\",} 1.0")
        .contains("longHistogram_units_sum{key1=\"value1\",} 1.0")
        .contains("longHistogram_units_count{key1=\"value2\",} 1.0")
        .contains("longHistogram_units_sum{key1=\"value2\",} 2.0")
        .contains("# HELP longHistogram_units_max LongHistogram test")
        .contains("# TYPE longHistogram_units_max gauge")
        .contains("longHistogram_units_max{key1=\"value1\",} 1.0")
        .contains("longHistogram_units_max{key1=\"value2\",} 2.0");

    longHistogram.record(2, Attributes.of(KEY1, "value1"));

    output = prometheusMeterRegistry.scrape();
    assertThat(output)
        .contains("longHistogram_units_count{key1=\"value1\",} 2.0")
        .contains("longHistogram_units_sum{key1=\"value1\",} 3.0")
        .contains("longHistogram_units_max{key1=\"value1\",} 2.0");
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
                  measurement.record(1.5, Attributes.of(KEY1, "value1"));
                  measurement.record(2.5, Attributes.of(KEY1, "value2"));
                })) {

      String output = scrapeFor("doubleGauge");
      assertThat(output)
          .contains("# TYPE doubleGauge_units gauge")
          .contains("# HELP doubleGauge_units DoubleGauge test")
          .contains("doubleGauge_units{key1=\"value1\",} 1.5")
          .contains("doubleGauge_units{key1=\"value2\",} 2.5");
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
                  measurement.record(1, Attributes.of(KEY1, "value1"));
                  measurement.record(2, Attributes.of(KEY1, "value2"));
                })) {

      String output = scrapeFor("longGauge");
      assertThat(output)
          .contains("# TYPE longGauge_units gauge")
          .contains("# HELP longGauge_units LongGauge test")
          .contains("longGauge_units{key1=\"value1\",} 1.0")
          .contains("longGauge_units{key1=\"value2\",} 2.0");
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
      boolean first = true;

      @Override
      public void accept(T t) {
        if (first) {
          first = false;
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
