/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;
import static io.opentelemetry.sdk.testing.assertj.metrics.MetricAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.testing.InMemoryMetricReader;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtelHelperAsynchronousMetricTest {

  // Will eventually be replaced with Jupiter extension in sdk-testing
  private SdkMeterProvider meterProvider;
  private InMemoryMetricReader metricReader;

  private OtelHelper otel;

  @BeforeEach
  void setUp() {
    metricReader = new InMemoryMetricReader();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    otel = new OtelHelper(null, new GroovyMetricEnvironment(meterProvider, "otel.test"));
  }

  @Test
  void doubleCounterCallback() {
    otel.doubleCounterCallback(
        "double-counter",
        "a double counter",
        "ms",
        result -> result.observe(123.456, Attributes.builder().put("key", "value").build()));

    otel.doubleCounterCallback(
        "my-double-counter",
        "another double counter",
        "µs",
        result -> result.observe(234.567, Attributes.builder().put("myKey", "myValue").build()));

    otel.doubleCounterCallback(
        "another-double-counter",
        "double counter",
        result ->
            result.observe(
                345.678, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.doubleCounterCallback(
        "yet-another-double-counter",
        result ->
            result.observe(
                456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-counter")
                    .hasDescription("a double counter")
                    .hasUnit("ms")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(123.456)
                                .attributes()
                                .containsOnly(attributeEntry("key", "value"))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-counter")
                    .hasDescription("another double counter")
                    .hasUnit("µs")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(234.567)
                                .attributes()
                                .containsOnly(attributeEntry("myKey", "myValue"))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-counter")
                    .hasDescription("double counter")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(345.678)
                                .attributes()
                                .containsOnly(attributeEntry("anotherKey", "anotherValue"))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(456.789)
                                .attributes()
                                .containsOnly(attributeEntry("yetAnotherKey", "yetAnotherValue"))));
  }

  @Test
  void doubleCounterCallbackMemoization() {
    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.observe(10.1, Attributes.builder().put("key1", "value1").build()));
    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.observe(20.2, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.observe(30.3, Attributes.builder().put("key3", "value3").build()));
    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.observe(40.4, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(20.2)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(40.4)
                                .attributes()
                                .containsOnly(attributeEntry("key4", "value4"))));
  }

  @Test
  void longCounterCallback() {
    otel.longCounterCallback(
        "long-counter",
        "a long counter",
        "ms",
        result -> result.observe(123, Attributes.builder().put("key", "value").build()));

    otel.longCounterCallback(
        "my-long-counter",
        "another long counter",
        "µs",
        result -> result.observe(234, Attributes.builder().put("myKey", "myValue").build()));

    otel.longCounterCallback(
        "another-long-counter",
        "long counter",
        result ->
            result.observe(345, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.longCounterCallback(
        "yet-another-long-counter",
        result ->
            result.observe(
                456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-counter")
                    .hasDescription("a long counter")
                    .hasUnit("ms")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(123)
                                .attributes()
                                .containsOnly(attributeEntry("key", "value"))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-counter")
                    .hasDescription("another long counter")
                    .hasUnit("µs")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(234)
                                .attributes()
                                .containsOnly(attributeEntry("myKey", "myValue"))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-counter")
                    .hasDescription("long counter")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(345)
                                .attributes()
                                .containsOnly(attributeEntry("anotherKey", "anotherValue"))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(456)
                                .attributes()
                                .containsOnly(attributeEntry("yetAnotherKey", "yetAnotherValue"))));
  }

  @Test
  void longCounterCallbackMemoization() {
    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.observe(10, Attributes.builder().put("key1", "value1").build()));
    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.observe(20, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.observe(30, Attributes.builder().put("key3", "value3").build()));
    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.observe(40, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(20)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(40)
                                .attributes()
                                .containsOnly(attributeEntry("key4", "value4"))));
  }

  @Test
  void doubleUpDownCounterCallback() {
    otel.doubleUpDownCounterCallback(
        "double-counter",
        "a double counter",
        "ms",
        result -> result.observe(-123.456, Attributes.builder().put("key", "value").build()));

    otel.doubleUpDownCounterCallback(
        "my-double-counter",
        "another double counter",
        "µs",
        result -> result.observe(-234.567, Attributes.builder().put("myKey", "myValue").build()));

    otel.doubleUpDownCounterCallback(
        "another-double-counter",
        "double counter",
        result ->
            result.observe(
                345.678, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.doubleUpDownCounterCallback(
        "yet-another-double-counter",
        result ->
            result.observe(
                456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-counter")
                    .hasDescription("a double counter")
                    .hasUnit("ms")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(-123.456)
                                .attributes()
                                .containsOnly(attributeEntry("key", "value"))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-counter")
                    .hasDescription("another double counter")
                    .hasUnit("µs")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(-234.567)
                                .attributes()
                                .containsOnly(attributeEntry("myKey", "myValue"))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-counter")
                    .hasDescription("double counter")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(345.678)
                                .attributes()
                                .containsOnly(attributeEntry("anotherKey", "anotherValue"))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(456.789)
                                .attributes()
                                .containsOnly(attributeEntry("yetAnotherKey", "yetAnotherValue"))));
  }

  @Test
  void doubleUpDownCounterCallbackMemoization() {
    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.observe(-10.1, Attributes.builder().put("key1", "value1").build()));
    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.observe(-20.2, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.observe(30.3, Attributes.builder().put("key3", "value3").build()));
    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.observe(40.4, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(-20.2)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(40.4)
                                .attributes()
                                .containsOnly(attributeEntry("key4", "value4"))));
  }

  @Test
  void longUpDownCounterCallback() {
    otel.longUpDownCounterCallback(
        "long-counter",
        "a long counter",
        "ms",
        result -> result.observe(-123, Attributes.builder().put("key", "value").build()));

    otel.longUpDownCounterCallback(
        "my-long-counter",
        "another long counter",
        "µs",
        result -> result.observe(-234, Attributes.builder().put("myKey", "myValue").build()));

    otel.longUpDownCounterCallback(
        "another-long-counter",
        "long counter",
        result ->
            result.observe(345, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.longUpDownCounterCallback(
        "yet-another-long-counter",
        result ->
            result.observe(
                456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-counter")
                    .hasDescription("a long counter")
                    .hasUnit("ms")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(-123)
                                .attributes()
                                .containsOnly(attributeEntry("key", "value"))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-counter")
                    .hasDescription("another long counter")
                    .hasUnit("µs")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(-234)
                                .attributes()
                                .containsOnly(attributeEntry("myKey", "myValue"))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-counter")
                    .hasDescription("long counter")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(345)
                                .attributes()
                                .containsOnly(attributeEntry("anotherKey", "anotherValue"))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(456)
                                .attributes()
                                .containsOnly(attributeEntry("yetAnotherKey", "yetAnotherValue"))));
  }

  @Test
  void longUpDownCounterCallbackMemoization() {
    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.observe(-10, Attributes.builder().put("key1", "value1").build()));
    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.observe(-20, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.observe(30, Attributes.builder().put("key3", "value3").build()));
    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.observe(40, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(-20)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSum()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(40)
                                .attributes()
                                .containsOnly(attributeEntry("key4", "value4"))));
  }

  @Test
  void doubleValueCallback() {
    otel.doubleValueCallback(
        "double-value",
        "a double value",
        "ms",
        result -> result.observe(123.456, Attributes.builder().put("key", "value").build()));

    otel.doubleValueCallback(
        "my-double-value",
        "another double value",
        "µs",
        result -> result.observe(234.567, Attributes.builder().put("myKey", "myValue").build()));

    otel.doubleValueCallback(
        "another-double-value",
        "double value",
        result ->
            result.observe(
                345.678, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.doubleValueCallback(
        "yet-another-double-value",
        result ->
            result.observe(
                456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-value")
                    .hasDescription("a double value")
                    .hasUnit("ms")
                    .hasDoubleGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(123.456)
                                .attributes()
                                .containsOnly(attributeEntry("key", "value"))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-value")
                    .hasDescription("another double value")
                    .hasUnit("µs")
                    .hasDoubleGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(234.567)
                                .attributes()
                                .containsOnly(attributeEntry("myKey", "myValue"))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-value")
                    .hasDescription("double value")
                    .hasUnit("1")
                    .hasDoubleGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(345.678)
                                .attributes()
                                .containsOnly(attributeEntry("anotherKey", "anotherValue"))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-value")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasDoubleGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(456.789)
                                .attributes()
                                .containsOnly(attributeEntry("yetAnotherKey", "yetAnotherValue"))));
  }

  @Test
  void doubleValueCallbackMemoization() {
    otel.doubleValueCallback(
        "dc",
        "double",
        result -> result.observe(10.1, Attributes.builder().put("key1", "value1").build()));
    otel.doubleValueCallback(
        "dc",
        "double",
        result -> result.observe(20.2, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.doubleValueCallback(
        "dc",
        "double",
        result -> result.observe(30.3, Attributes.builder().put("key3", "value3").build()));
    otel.doubleValueCallback(
        "dc",
        "double",
        result -> {
          result.observe(40.4, Attributes.builder().put("key4", "value4").build());
          result.observe(50.5, Attributes.builder().put("key2", "value2").build());
        });
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(20.2)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleGauge()
                    .points()
                    .satisfiesExactlyInAnyOrder(
                        point ->
                            assertThat(point)
                                .hasValue(40.4)
                                .attributes()
                                .containsOnly(attributeEntry("key4", "value4")),
                        point ->
                            assertThat(point)
                                .hasValue(50.5)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));
  }

  @Test
  void longValueCallback() {
    otel.longValueCallback(
        "long-value",
        "a long value",
        "ms",
        result -> result.observe(123, Attributes.builder().put("key", "value").build()));

    otel.longValueCallback(
        "my-long-value",
        "another long value",
        "µs",
        result -> result.observe(234, Attributes.builder().put("myKey", "myValue").build()));

    otel.longValueCallback(
        "another-long-value",
        "long value",
        result ->
            result.observe(345, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.longValueCallback(
        "yet-another-long-value",
        result ->
            result.observe(
                456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-value")
                    .hasDescription("a long value")
                    .hasUnit("ms")
                    .hasLongGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(123)
                                .attributes()
                                .containsOnly(attributeEntry("key", "value"))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-value")
                    .hasDescription("another long value")
                    .hasUnit("µs")
                    .hasLongGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(234)
                                .attributes()
                                .containsOnly(attributeEntry("myKey", "myValue"))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-value")
                    .hasDescription("long value")
                    .hasUnit("1")
                    .hasLongGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(345)
                                .attributes()
                                .containsOnly(attributeEntry("anotherKey", "anotherValue"))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-value")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasLongGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(456)
                                .attributes()
                                .containsOnly(attributeEntry("yetAnotherKey", "yetAnotherValue"))));
  }

  @Test
  void longValueCallbackMemoization() {
    otel.longValueCallback(
        "dc",
        "long",
        result -> result.observe(10, Attributes.builder().put("key1", "value1").build()));
    otel.longValueCallback(
        "dc",
        "long",
        result -> result.observe(20, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.longValueCallback(
        "dc",
        "long",
        result -> result.observe(30, Attributes.builder().put("key3", "value3").build()));
    otel.longValueCallback(
        "dc",
        "long",
        result -> result.observe(40, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(20)
                                .attributes()
                                .containsOnly(attributeEntry("key2", "value2"))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongGauge()
                    .points()
                    .satisfiesExactly(
                        point ->
                            assertThat(point)
                                .hasValue(40)
                                .attributes()
                                .containsOnly(attributeEntry("key4", "value4"))));
  }
}
