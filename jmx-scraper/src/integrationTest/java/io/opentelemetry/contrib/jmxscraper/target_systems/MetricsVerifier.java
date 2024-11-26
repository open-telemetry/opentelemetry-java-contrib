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

  @CanIgnoreReturnValue
  public MetricsVerifier register(String metricName, Consumer<Metric> assertion) {
    assertions.put(metricName, assertion);
    return this;
  };

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertGauge(String metricName, String description, String unit) {
    return register(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithGauge(metric);
          assertThat(metric.getGauge().getDataPointsList())
              .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
        });
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertCounter(String metricName, String description, String unit) {
    return assertSum(metricName, description, unit, /* isMonotonic= */ true);
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertUpDownCounter(String metricName, String description, String unit) {
    return assertSum(metricName, description, unit, /* isMonotonic= */ false);
  }

  @SafeVarargs
  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public final MetricsVerifier assertGaugeWithAttributes(
      String metricName,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return register(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithGauge(metric);
          assertAttributedPoints(
              metricName, metric.getGauge().getDataPointsList(), attributeGroupAssertions);
        });
  }

  @SafeVarargs
  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public final MetricsVerifier assertCounterWithAttributes(
      String metricName,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return assertSumWithAttributes(
        metricName, description, unit, /* isMonotonic= */ true, attributeGroupAssertions);
  }

  @SafeVarargs
  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public final MetricsVerifier assertUpDownCounterWithAttributes(
      String metricName,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return assertSumWithAttributes(
        metricName, description, unit, /* isMonotonic= */ false, attributeGroupAssertions);
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertTypedCounter(
      String metricName, String description, String unit, List<String> types) {
    return register(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithSum(metric, /* isMonotonic= */ true);
          assertTypedPoints(metricName, metric.getSum().getDataPointsList(), types);
        });
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  public MetricsVerifier assertTypedGauge(
      String metricName, String description, String unit, List<String> types) {
    return register(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithGauge(metric);
          assertTypedPoints(metricName, metric.getGauge().getDataPointsList(), types);
        });
  }

  public void verify(List<Metric> metrics) {
    verifyAllExpectedMetricsWereReceived(metrics);

    Set<String> unverifiedMetrics = new HashSet<>();

    for (Metric metric : metrics) {
      String metricName = metric.getName();
      Consumer<Metric> assertion = assertions.get(metricName);

      if (assertion != null) {
        assertion.accept(metric);
      } else {
        unverifiedMetrics.add(metricName);
      }
    }

    if (strictMode && !unverifiedMetrics.isEmpty()) {
      fail("Metrics received but not verified because no assertion exists: " + unverifiedMetrics);
    }
  }

  @SuppressWarnings("SystemOut")
  private void verifyAllExpectedMetricsWereReceived(List<Metric> metrics) {
    Set<String> receivedMetricNames =
        metrics.stream().map(Metric::getName).collect(Collectors.toSet());
    Set<String> assertionNames = new HashSet<>(assertions.keySet());

    assertionNames.removeAll(receivedMetricNames);
    if (!assertionNames.isEmpty()) {
      fail("Metrics expected but not received: " + assertionNames);
    }
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  private MetricsVerifier assertSum(
      String metricName, String description, String unit, boolean isMonotonic) {
    return register(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithSum(metric, isMonotonic);
          assertThat(metric.getSum().getDataPointsList())
              .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
        });
  }

  @SafeVarargs
  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  private final MetricsVerifier assertSumWithAttributes(
      String metricName,
      String description,
      String unit,
      boolean isMonotonic,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    assertions.put(
        metricName,
        metric -> {
          assertDescription(metric, description);
          assertUnit(metric, unit);
          assertMetricWithSum(metric, isMonotonic);
          assertAttributedPoints(
              metricName, metric.getSum().getDataPointsList(), attributeGroupAssertions);
        });

    return this;
  }

  private static void assertMetricWithGauge(Metric metric) {
    assertThat(metric.hasGauge()).withFailMessage("Metric with gauge expected").isTrue();
  }

  private static void assertMetricWithSum(Metric metric, boolean isMonotonic) {
    assertThat(metric.hasSum()).withFailMessage("Metric with sum expected").isTrue();
    assertThat(metric.getSum().getIsMonotonic())
        .withFailMessage((isMonotonic ? "Monotonic" : "Non monotonic") + " sum expected")
        .isEqualTo(isMonotonic);
  }

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
