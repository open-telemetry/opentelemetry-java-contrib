/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.MapAssert;

/** Metrics assertions */
class MetricAssertions {

  private MetricAssertions() {}

  static void assertGauge(Metric metric, String name, String description, String unit) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithGauge(metric);
    assertThat(metric.getGauge().getDataPointsList())
        .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
  }

  static void assertSum(Metric metric, String name, String description, String unit) {
    assertSum(metric, name, description, unit, /* isMonotonic= */ true);
  }

  static void assertSum(
      Metric metric, String name, String description, String unit, boolean isMonotonic) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithSum(metric, isMonotonic);
    assertThat(metric.getSum().getDataPointsList())
        .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
  }

  static void assertTypedGauge(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithGauge(metric);
    assertTypedPoints(metric.getGauge().getDataPointsList(), types);
  }

  static void assertTypedSum(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithSum(metric);
    assertTypedPoints(metric.getSum().getDataPointsList(), types);
  }

  @SafeVarargs
  static void assertSumWithAttributes(
      Metric metric,
      String name,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    assertSumWithAttributes(
        metric, name, description, unit, /* isMonotonic= */ true, attributeGroupAssertions);
  }

  @SafeVarargs
  static void assertSumWithAttributes(
      Metric metric,
      String name,
      String description,
      String unit,
      boolean isMonotonic,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithSum(metric, isMonotonic);
    assertAttributedPoints(metric.getSum().getDataPointsList(), attributeGroupAssertions);
  }

  @SafeVarargs
  static void assertSumWithAttributesMultiplePoints(
      Metric metric,
      String name,
      String description,
      String unit,
      boolean isMonotonic,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithSum(metric, isMonotonic);
    assertAttributedMultiplePoints(metric.getSum().getDataPointsList(), attributeGroupAssertions);
  }

  @SafeVarargs
  static void assertGaugeWithAttributes(
      Metric metric,
      String name,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertMetricWithGauge(metric);
    assertAttributedPoints(metric.getGauge().getDataPointsList(), attributeGroupAssertions);
  }

  private static void assertMetricWithGauge(Metric metric) {
    assertThat(metric.hasGauge()).withFailMessage("Metric with gauge expected").isTrue();
  }

  private static void assertMetricWithSum(Metric metric) {
    assertThat(metric.hasSum()).withFailMessage("Metric with sum expected").isTrue();
  }

  private static void assertMetricWithSum(Metric metric, boolean isMonotonic) {
    assertMetricWithSum(metric);
    assertThat(metric.getSum().getIsMonotonic())
        .withFailMessage("Metric should " + (isMonotonic ? "" : "not ") + "be monotonic")
        .isEqualTo(isMonotonic);
  }

  @SuppressWarnings("unchecked")
  private static void assertTypedPoints(List<NumberDataPoint> points, List<String> types) {
    Consumer<MapAssert<String, String>>[] assertions =
        types.stream()
            .map(
                type ->
                    (Consumer<MapAssert<String, String>>)
                        attrs -> attrs.containsOnly(entry("name", type)))
            .toArray(Consumer[]::new);

    assertAttributedPoints(points, assertions);
  }

  @SuppressWarnings("unchecked")
  private static void assertAttributedPoints(
      List<NumberDataPoint> points,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    Consumer<Map<String, String>>[] assertions =
        Arrays.stream(attributeGroupAssertions)
            .map(assertion -> (Consumer<Map<String, String>>) m -> assertion.accept(assertThat(m)))
            .toArray(Consumer[]::new);

    assertThat(points)
        .withFailMessage("Invalid metric attributes. Actual: " + points)
        .extracting(
            numberDataPoint ->
                numberDataPoint.getAttributesList().stream()
                    .collect(
                        Collectors.toMap(
                            KeyValue::getKey, keyValue -> keyValue.getValue().getStringValue())))
        .satisfiesExactlyInAnyOrder(assertions);
  }

  @SuppressWarnings("unchecked")
  private static void assertAttributedMultiplePoints(
      List<NumberDataPoint> points,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {

    points.stream()
        .map(NumberDataPoint::getAttributesList)
        .forEach(
            kvList -> {
              Map<String, String> kvMap =
                  kvList.stream()
                      .collect(
                          Collectors.toMap(KeyValue::getKey, kv -> kv.getValue().getStringValue()));
              Arrays.stream(attributeGroupAssertions)
                  .forEach(assertion -> assertion.accept(assertThat(kvMap)));
            });
  }
}
