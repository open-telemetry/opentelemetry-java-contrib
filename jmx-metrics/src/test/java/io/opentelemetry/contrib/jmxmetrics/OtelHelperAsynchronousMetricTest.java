/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
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
    metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    otel = new OtelHelper(null, new GroovyMetricEnvironment(meterProvider, "otel.test"), false);
  }

  @Test
  void doubleCounterCallback() {
    otel.doubleCounterCallback(
        "double-counter",
        "a double counter",
        "ms",
        result -> result.record(123.456, Attributes.builder().put("key", "value").build()));

    otel.doubleCounterCallback(
        "my-double-counter",
        "another double counter",
        "us",
        result -> result.record(234.567, Attributes.builder().put("myKey", "myValue").build()));

    otel.doubleCounterCallback(
        "another-double-counter",
        "double counter",
        result ->
            result.record(345.678, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.doubleCounterCallback(
        "yet-another-double-counter",
        result ->
            result.record(
                456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-counter")
                    .hasDescription("a double counter")
                    .hasUnit("ms")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(123.456)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-counter")
                    .hasDescription("another double counter")
                    .hasUnit("us")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(234.567)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-counter")
                    .hasDescription("double counter")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(345.678)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(456.789)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void doubleCounterCallbackMemoization() {
    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.record(10.1, Attributes.builder().put("key1", "value1").build()));
    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.record(20.2, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.record(30.3, Attributes.builder().put("key3", "value3").build()));
    otel.doubleCounterCallback(
        "dc",
        "double",
        result -> result.record(40.4, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(20.2)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(40.4)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key4"), "value4")))));
  }

  @Test
  void longCounterCallback() {
    otel.longCounterCallback(
        "long-counter",
        "a long counter",
        "ms",
        result -> result.record(123, Attributes.builder().put("key", "value").build()));

    otel.longCounterCallback(
        "my-long-counter",
        "another long counter",
        "us",
        result -> result.record(234, Attributes.builder().put("myKey", "myValue").build()));

    otel.longCounterCallback(
        "another-long-counter",
        "long counter",
        result ->
            result.record(345, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.longCounterCallback(
        "yet-another-long-counter",
        result ->
            result.record(
                456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-counter")
                    .hasDescription("a long counter")
                    .hasUnit("ms")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(123)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-counter")
                    .hasDescription("another long counter")
                    .hasUnit("us")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(234)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-counter")
                    .hasDescription("long counter")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(345)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(456)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void longCounterCallbackMemoization() {
    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.record(10, Attributes.builder().put("key1", "value1").build()));
    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.record(20, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.record(30, Attributes.builder().put("key3", "value3").build()));
    otel.longCounterCallback(
        "dc",
        "long",
        result -> result.record(40, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(20)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(40)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key4"), "value4")))));
  }

  @Test
  void doubleUpDownCounterCallback() {
    otel.doubleUpDownCounterCallback(
        "double-counter",
        "a double counter",
        "ms",
        result -> result.record(-123.456, Attributes.builder().put("key", "value").build()));

    otel.doubleUpDownCounterCallback(
        "my-double-counter",
        "another double counter",
        "us",
        result -> result.record(-234.567, Attributes.builder().put("myKey", "myValue").build()));

    otel.doubleUpDownCounterCallback(
        "another-double-counter",
        "double counter",
        result ->
            result.record(345.678, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.doubleUpDownCounterCallback(
        "yet-another-double-counter",
        result ->
            result.record(
                456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-counter")
                    .hasDescription("a double counter")
                    .hasUnit("ms")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-123.456)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-counter")
                    .hasDescription("another double counter")
                    .hasUnit("us")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-234.567)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-counter")
                    .hasDescription("double counter")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(345.678)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(456.789)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void doubleUpDownCounterCallbackMemoization() {
    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.record(-10.1, Attributes.builder().put("key1", "value1").build()));
    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.record(-20.2, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.record(30.3, Attributes.builder().put("key3", "value3").build()));
    otel.doubleUpDownCounterCallback(
        "dc",
        "double",
        result -> result.record(40.4, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-20.2)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(40.4)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key4"), "value4")))));
  }

  @Test
  void longUpDownCounterCallback() {
    otel.longUpDownCounterCallback(
        "long-counter",
        "a long counter",
        "ms",
        result -> result.record(-123, Attributes.builder().put("key", "value").build()));

    otel.longUpDownCounterCallback(
        "my-long-counter",
        "another long counter",
        "us",
        result -> result.record(-234, Attributes.builder().put("myKey", "myValue").build()));

    otel.longUpDownCounterCallback(
        "another-long-counter",
        "long counter",
        result ->
            result.record(345, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.longUpDownCounterCallback(
        "yet-another-long-counter",
        result ->
            result.record(
                456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-counter")
                    .hasDescription("a long counter")
                    .hasUnit("ms")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-123)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-counter")
                    .hasDescription("another long counter")
                    .hasUnit("us")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-234)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-counter")
                    .hasDescription("long counter")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(345)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-counter")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(456)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void longUpDownCounterCallbackMemoization() {
    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.record(-10, Attributes.builder().put("key1", "value1").build()));
    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.record(-20, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.record(30, Attributes.builder().put("key3", "value3").build()));
    otel.longUpDownCounterCallback(
        "dc",
        "long",
        result -> result.record(40, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-20)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(40)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key4"), "value4")))));
  }

  @Test
  void doubleValueCallback() {
    otel.doubleValueCallback(
        "double-value",
        "a double value",
        "ms",
        result -> result.record(123.456, Attributes.builder().put("key", "value").build()));

    otel.doubleValueCallback(
        "my-double-value",
        "another double value",
        "us",
        result -> result.record(234.567, Attributes.builder().put("myKey", "myValue").build()));

    otel.doubleValueCallback(
        "another-double-value",
        "double value",
        result ->
            result.record(345.678, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.doubleValueCallback(
        "yet-another-double-value",
        result ->
            result.record(
                456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-value")
                    .hasDescription("a double value")
                    .hasUnit("ms")
                    .hasDoubleGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(123.456)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-value")
                    .hasDescription("another double value")
                    .hasUnit("us")
                    .hasDoubleGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(234.567)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-value")
                    .hasDescription("double value")
                    .hasUnit("1")
                    .hasDoubleGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(345.678)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-value")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasDoubleGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(456.789)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void doubleValueCallbackMemoization() {
    otel.doubleValueCallback(
        "dc",
        "double",
        result -> result.record(10.1, Attributes.builder().put("key1", "value1").build()));
    otel.doubleValueCallback(
        "dc",
        "double",
        result -> result.record(20.2, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.doubleValueCallback(
        "dc",
        "double",
        result -> result.record(30.3, Attributes.builder().put("key3", "value3").build()));
    otel.doubleValueCallback(
        "dc",
        "double",
        result -> {
          result.record(40.4, Attributes.builder().put("key4", "value4").build());
          result.record(50.5, Attributes.builder().put("key2", "value2").build());
        });
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(20.2)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("double")
                    .hasUnit("1")
                    .hasDoubleGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(40.4)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key4"), "value4")),
                                point ->
                                    point
                                        .hasValue(50.5)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));
  }

  @Test
  void longValueCallback() {
    otel.longValueCallback(
        "long-value",
        "a long value",
        "ms",
        result -> result.record(123, Attributes.builder().put("key", "value").build()));

    otel.longValueCallback(
        "my-long-value",
        "another long value",
        "us",
        result -> result.record(234, Attributes.builder().put("myKey", "myValue").build()));

    otel.longValueCallback(
        "another-long-value",
        "long value",
        result ->
            result.record(345, Attributes.builder().put("anotherKey", "anotherValue").build()));

    otel.longValueCallback(
        "yet-another-long-value",
        result ->
            result.record(
                456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build()));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-value")
                    .hasDescription("a long value")
                    .hasUnit("ms")
                    .hasLongGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(123)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-value")
                    .hasDescription("another long value")
                    .hasUnit("us")
                    .hasLongGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(234)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-value")
                    .hasDescription("long value")
                    .hasUnit("1")
                    .hasLongGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(345)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-value")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasLongGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(456)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void longValueCallbackMemoization() {
    otel.longValueCallback(
        "dc",
        "long",
        result -> result.record(10, Attributes.builder().put("key1", "value1").build()));
    otel.longValueCallback(
        "dc",
        "long",
        result -> result.record(20, Attributes.builder().put("key2", "value2").build()));
    Collection<MetricData> firstMetrics = metricReader.collectAllMetrics();

    otel.longValueCallback(
        "dc",
        "long",
        result -> result.record(30, Attributes.builder().put("key3", "value3").build()));
    otel.longValueCallback(
        "dc",
        "long",
        result -> result.record(40, Attributes.builder().put("key4", "value4").build()));
    Collection<MetricData> secondMetrics = metricReader.collectAllMetrics();

    assertThat(firstMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(20)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key2"), "value2")))));

    assertThat(secondMetrics)
        .satisfiesExactly(
            metric ->
                assertThat(metric)
                    .hasName("dc")
                    .hasDescription("long")
                    .hasUnit("1")
                    .hasLongGaugeSatisfying(
                        gauge ->
                            gauge.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(40)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key4"), "value4")))));
  }
}
