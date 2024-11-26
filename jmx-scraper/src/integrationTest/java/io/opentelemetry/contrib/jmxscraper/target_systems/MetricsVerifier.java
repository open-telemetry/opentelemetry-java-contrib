/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.jmxscraper.assertions.MetricAssert;
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

  @CanIgnoreReturnValue
  public MetricsVerifier allowExtraMetrics() {
    strictMode = false;
    return this;
  }

  @CanIgnoreReturnValue
  private MetricsVerifier registerAssert(String metricName, Consumer<Metric> assertion) {
    assertions.put(metricName, assertion);
    return this;
  }

  @CanIgnoreReturnValue
  public MetricsVerifier register(String metricName, Consumer<MetricAssert> assertion) {
    assertions.put(metricName, metric -> assertion.accept(assertThat(metric)));
    return this;
  }

  // TODO: can now be inlined
  @CanIgnoreReturnValue
  public MetricsVerifier assertGauge(String metricName, String description, String unit) {
    return register(
        metricName,
        metric ->
            metric
                .hasDescription(description)
                .hasUnit(unit)
                .isGauge()
                .hasDataPointsWithoutAttributes());
  }

  // TODO: can now be inlined
  @CanIgnoreReturnValue
  public MetricsVerifier assertCounter(String metricName, String description, String unit) {
    return register(
        metricName,
        metric ->
            metric
                .hasDescription(description)
                .hasUnit(unit)
                .isCounter()
                .hasDataPointsWithoutAttributes());
  }

  // TODO: can now be inlined
  @CanIgnoreReturnValue
  public MetricsVerifier assertUpDownCounter(String metricName, String description, String unit) {
    return register(
        metricName,
        metric ->
            metric
                .hasDescription(description)
                .hasUnit(unit)
                .isUpDownCounter()
                .hasDataPointsWithoutAttributes());
  }

  @SafeVarargs
  @CanIgnoreReturnValue
  public final MetricsVerifier assertGaugeWithAttributes(
      String metricName,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return registerAssert(
        metricName,
        metric -> {
          assertThat(metric).hasDescription(description).hasUnit(unit).isGauge();

          assertAttributedPoints(
              metricName, metric.getGauge().getDataPointsList(), attributeGroupAssertions);
        });
  }

  @SafeVarargs
  @CanIgnoreReturnValue
  public final MetricsVerifier assertCounterWithAttributes(
      String metricName,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return assertSumWithAttributes(
        metricName, description, unit, /* isMonotonic= */ true, attributeGroupAssertions);
  }

  @SafeVarargs
  @CanIgnoreReturnValue
  public final MetricsVerifier assertUpDownCounterWithAttributes(
      String metricName,
      String description,
      String unit,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return assertSumWithAttributes(
        metricName, description, unit, /* isMonotonic= */ false, attributeGroupAssertions);
  }

  @CanIgnoreReturnValue
  public MetricsVerifier assertTypedCounter(
      String metricName, String description, String unit, List<String> types) {
    return registerAssert(
        metricName,
        metric ->
            assertThat(metric)
                .hasDescription(description)
                .hasUnit(unit)
                .isCounter()
                .hasTypedDataPoints(types));
  }

  @CanIgnoreReturnValue
  public MetricsVerifier assertTypedGauge(
      String metricName, String description, String unit, List<String> types) {
    return registerAssert(
        metricName,
        metric ->
            assertThat(metric)
                .hasDescription(description)
                .hasUnit(unit)
                .isGauge()
                .hasTypedDataPoints(types));
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

  @SafeVarargs
  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  private final MetricsVerifier assertSumWithAttributes(
      String metricName,
      String description,
      String unit,
      boolean isMonotonic,
      Consumer<MapAssert<String, String>>... attributeGroupAssertions) {
    return registerAssert(
        metricName,
        metric -> {
          assertThat(metric).hasDescription(description).hasUnit(unit).hasSum(isMonotonic);

          assertAttributedPoints(
              metricName, metric.getSum().getDataPointsList(), attributeGroupAssertions);
        });
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
