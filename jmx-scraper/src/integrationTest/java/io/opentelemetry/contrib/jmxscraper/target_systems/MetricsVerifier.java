/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.MapAssert;

public class MetricsVerifier {
  private static final String METRIC_VERIFICATION_FAILURE_MESSAGE =
      "Verification of %s metric failed";

  private final Map<String, Consumer<Metric>> assertions = new HashMap<>();
  private boolean strictMode = true;

  private MetricsVerifier() {}

  /**
   * Create instance of MetricsVerifier configured to fail verification if any metric was not
   * verified because there is no assertion defined for it. This behavior can be changed by calling
   * allowingExtraMetrics() method.
   *
   * @return new instance of MetricsVerifier
   * @see #allowExtraMetrics()
   */
  public static MetricsVerifier create() {
    return new MetricsVerifier();
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier allowExtraMetrics() {
    strictMode = false;
    return this;
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertGauge(String metricName, String description, String unit) {
    assertions.put(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithGauge(metric);
          assertThat(metric.getGauge().getDataPointsList())
              .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
        });

    return this;
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertTypedSum(
      String metricName, String description, String unit, List<String> types) {
    assertions.put(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithSum(metric);
          assertTypedPoints(metricName, metric.getSum().getDataPointsList(), types);
        });

    return this;
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertTypedGauge(
      String metricName, String description, String unit, List<String> types) {
    assertions.put(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithGauge(metric);
          assertTypedPoints(metricName, metric.getGauge().getDataPointsList(), types);
        });

    return this;
  }

  public void verify(List<Metric> metrics) {
    Set<String> unverifiedMetrics = new HashSet<>();
    Set<String> skippedAssertions = assertions.keySet();

    for (Metric metric : metrics) {
      String metricName = metric.getName();
      Consumer<Metric> assertion = assertions.get(metricName);

      if (assertion != null) {
        assertion.accept(metric);
        skippedAssertions.remove(metricName);
      } else {
        unverifiedMetrics.add(metricName);
      }
    }

    if (!skippedAssertions.isEmpty()) {
      fail("The following metrics was expected but not received: " + skippedAssertions);
    }
    if (strictMode && !unverifiedMetrics.isEmpty()) {
      fail("The following metrics was received but not verified: " + unverifiedMetrics);
    }
  }

  private static void assertMetricWithGauge(Metric metric) {
    assertThat(metric.hasGauge()).withFailMessage("Metric with gauge expected").isTrue();
  }

  private static void assertMetricWithSum(Metric metric) {
    assertThat(metric.hasSum()).withFailMessage("Metric with sum expected").isTrue();
  }

  //  private static void assertMetricWithSum(Metric metric, boolean isMonotonic) {
  //    assertMetricWithSum(metric);
  //    assertThat(metric.getSum().getIsMonotonic())
  //        .withFailMessage("Metric should " + (isMonotonic ? "" : "not ") + "be monotonic")
  //        .isEqualTo(isMonotonic);
  //  }

  private static void assertDescription(Metric metric, String expectedDescription) {
    assertThat(metric.getDescription())
        .describedAs(METRIC_VERIFICATION_FAILURE_MESSAGE, metric.getName())
        .withFailMessage(
            "\nExpected description: %s\n  Actual description: %s",
            expectedDescription, metric.getDescription())
        .isEqualTo(expectedDescription);
  }

  private static void assertUnit(Metric metric, String expectedUnit) {
    assertThat(metric.getUnit())
        .describedAs(METRIC_VERIFICATION_FAILURE_MESSAGE, metric.getName())
        .withFailMessage("\nExpected unit: %s\n  Actual unit: %s", expectedUnit, metric.getUnit())
        .isEqualTo(expectedUnit);
  }

  @SuppressWarnings("unchecked")
  private static void assertTypedPoints(
      String metricName, List<NumberDataPoint> points, List<String> types) {
    Consumer<MapAssert<String, String>>[] assertions =
        types.stream()
            .map(
                type ->
                    (Consumer<MapAssert<String, String>>)
                        attrs -> attrs.containsOnly(entry("name", type)))
            .toArray(Consumer[]::new);

    assertAttributedPoints(metricName, points, assertions);
  }

  @SuppressWarnings("unchecked")
  private static void assertAttributedPoints(
      String metricName,
      List<NumberDataPoint> points,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    Consumer<Map<String, String>>[] assertions =
        Arrays.stream(attributeGroupAssertions)
            .map(assertion -> (Consumer<Map<String, String>>) m -> assertion.accept(assertThat(m)))
            .toArray(Consumer[]::new);

    assertThat(points)
        .describedAs(METRIC_VERIFICATION_FAILURE_MESSAGE, metricName)
        .extracting(
            numberDataPoint ->
                numberDataPoint.getAttributesList().stream()
                    .collect(
                        Collectors.toMap(
                            KeyValue::getKey, keyValue -> keyValue.getValue().getStringValue())))
        .satisfiesExactlyInAnyOrder(assertions);
  }
}
