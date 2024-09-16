/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtelHelperSynchronousMetricTest {

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
  void doubleCounter() {
    DoubleCounter dc = otel.doubleCounter("double-counter", "a double counter", "ms");
    dc.add(123.456, Attributes.builder().put("key", "value").build());

    dc = otel.doubleCounter("my-double-counter", "another double counter", "us");
    dc.add(234.567, Attributes.builder().put("myKey", "myValue").build());

    dc = otel.doubleCounter("another-double-counter", "double counter");
    dc.add(345.678, Attributes.builder().put("anotherKey", "anotherValue").build());

    dc = otel.doubleCounter("yet-another-double-counter");
    dc.add(456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build());

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
  void longCounter() {
    LongCounter lc = otel.longCounter("long-counter", "a long counter", "ms");
    lc.add(123, Attributes.builder().put("key", "value").build());

    lc = otel.longCounter("my-long-counter", "another long counter", "us");
    lc.add(234, Attributes.builder().put("myKey", "myValue").build());

    lc = otel.longCounter("another-long-counter", "long counter");
    lc.add(345, Attributes.builder().put("anotherKey", "anotherValue").build());

    lc = otel.longCounter("yet-another-long-counter");
    lc.add(456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build());

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
  void doubleUpDownCounter() {
    DoubleUpDownCounter dudc =
        otel.doubleUpDownCounter("double-up-down-counter", "a double up-down-counter", "ms");
    dudc.add(-234.567, Attributes.builder().put("key", "value").build());

    dudc =
        otel.doubleUpDownCounter(
            "my-double-up-down-counter", "another double up-down-counter", "us");
    dudc.add(-123.456, Attributes.builder().put("myKey", "myValue").build());

    dudc = otel.doubleUpDownCounter("another-double-up-down-counter", "double up-down-counter");
    dudc.add(345.678, Attributes.builder().put("anotherKey", "anotherValue").build());

    dudc = otel.doubleUpDownCounter("yet-another-double-up-down-counter");
    dudc.add(456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build());

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-up-down-counter")
                    .hasDescription("a double up-down-counter")
                    .hasUnit("ms")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-234.567)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-up-down-counter")
                    .hasDescription("another double up-down-counter")
                    .hasUnit("us")
                    .hasDoubleSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-123.456)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-up-down-counter")
                    .hasDescription("double up-down-counter")
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
                    .hasName("yet-another-double-up-down-counter")
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
  void longUpDownCounter() {
    LongUpDownCounter ludc =
        otel.longUpDownCounter("long-up-down-counter", "a long up-down-counter", "ms");
    ludc.add(-234, Attributes.builder().put("key", "value").build());

    ludc = otel.longUpDownCounter("my-long-up-down-counter", "another long up-down-counter", "us");
    ludc.add(-123, Attributes.builder().put("myKey", "myValue").build());

    ludc = otel.longUpDownCounter("another-long-up-down-counter", "long up-down-counter");
    ludc.add(345, Attributes.builder().put("anotherKey", "anotherValue").build());

    ludc = otel.longUpDownCounter("yet-another-long-up-down-counter");
    ludc.add(456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build());

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-up-down-counter")
                    .hasDescription("a long up-down-counter")
                    .hasUnit("ms")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-234)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-up-down-counter")
                    .hasDescription("another long up-down-counter")
                    .hasUnit("us")
                    .hasLongSumSatisfying(
                        sum ->
                            sum.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasValue(-123)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-up-down-counter")
                    .hasDescription("long up-down-counter")
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
                    .hasName("yet-another-long-up-down-counter")
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
  void doubleHistogram() {
    DoubleHistogram dh = otel.doubleHistogram("double-histogram", "a double histogram", "ms");
    dh.record(234.567, Attributes.builder().put("key", "value").build());

    dh = otel.doubleHistogram("my-double-histogram", "another double histogram", "us");
    dh.record(123.456, Attributes.builder().put("myKey", "myValue").build());

    dh = otel.doubleHistogram("another-double-histogram", "double histogram");
    dh.record(345.678, Attributes.builder().put("anotherKey", "anotherValue").build());

    dh = otel.doubleHistogram("yet-another-double-histogram");
    dh.record(456.789, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build());

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("double-histogram")
                    .hasDescription("a double histogram")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(234.567)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-double-histogram")
                    .hasDescription("another double histogram")
                    .hasUnit("us")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(123.456)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-double-histogram")
                    .hasDescription("double histogram")
                    .hasUnit("1")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(345.678)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-double-histogram")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(456.789)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }

  @Test
  void longHistogram() {
    LongHistogram lh = otel.longHistogram("long-histogram", "a long histogram", "ms");
    lh.record(234, Attributes.builder().put("key", "value").build());

    lh = otel.longHistogram("my-long-histogram", "another long histogram", "us");
    lh.record(123, Attributes.builder().put("myKey", "myValue").build());

    lh = otel.longHistogram("another-long-histogram", "long histogram");
    lh.record(345, Attributes.builder().put("anotherKey", "anotherValue").build());

    lh = otel.longHistogram("yet-another-long-histogram");
    lh.record(456, Attributes.builder().put("yetAnotherKey", "yetAnotherValue").build());

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("long-histogram")
                    .hasDescription("a long histogram")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(234)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("key"), "value")))),
            metric ->
                assertThat(metric)
                    .hasName("my-long-histogram")
                    .hasDescription("another long histogram")
                    .hasUnit("us")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(123)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(AttributeKey.stringKey("myKey"), "myValue")))),
            metric ->
                assertThat(metric)
                    .hasName("another-long-histogram")
                    .hasDescription("long histogram")
                    .hasUnit("1")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(345)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("anotherKey"),
                                                "anotherValue")))),
            metric ->
                assertThat(metric)
                    .hasName("yet-another-long-histogram")
                    .hasDescription("")
                    .hasUnit("1")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(456)
                                        .hasCount(1)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                AttributeKey.stringKey("yetAnotherKey"),
                                                "yetAnotherValue")))));
  }
}
