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
    assertThat(metric.hasGauge()).isTrue();
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
    assertThat(metric.hasSum()).isTrue();
    assertThat(metric.getSum().getDataPointsList())
        .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
    assertThat(metric.getSum().getIsMonotonic()).isEqualTo(isMonotonic);
  }

  static void assertTypedGauge(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasGauge()).isTrue();
    assertTypedPoints(metric.getGauge().getDataPointsList(), types);
  }

  static void assertTypedSum(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasSum()).isTrue();
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
    assertThat(metric.hasSum()).describedAs("sum expected").isTrue();
    assertThat(metric.getSum().getIsMonotonic()).isEqualTo(isMonotonic);
    assertAttributedPoints(metric.getSum().getDataPointsList(), attributeGroupAssertions);
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
    assertThat(metric.hasGauge()).isTrue();
    assertAttributedPoints(metric.getGauge().getDataPointsList(), attributeGroupAssertions);
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
        .extracting(
            numberDataPoint ->
                numberDataPoint.getAttributesList().stream()
                    .collect(
                        Collectors.toMap(
                            KeyValue::getKey, keyValue -> keyValue.getValue().getStringValue())))
        .satisfiesExactlyInAnyOrder(assertions);
  }
}
