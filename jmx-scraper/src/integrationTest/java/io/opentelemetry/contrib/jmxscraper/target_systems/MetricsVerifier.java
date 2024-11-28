/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.jmxscraper.assertions.MetricAssert;
import io.opentelemetry.proto.metrics.v1.Metric;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MetricsVerifier {

  private final Map<String, Consumer<Metric>> assertions = new HashMap<>();
  private boolean strictMode = true;

  private MetricsVerifier() {}

  /**
   * Create instance of MetricsVerifier configured to fail verification if any metric was not
   * verified because there is no assertion defined for it. This behavior can be changed by calling
   * allowingExtraMetrics() method.
   *
   * @return new instance of MetricsVerifier
   * @see #disableStrictMode()
   */
  public static MetricsVerifier create() {
    return new MetricsVerifier();
  }

  @CanIgnoreReturnValue
  public MetricsVerifier disableStrictMode() {
    strictMode = false;
    return this;
  }

  @CanIgnoreReturnValue
  public MetricsVerifier add(String metricName, Consumer<MetricAssert> assertion) {
    assertions.put(
        metricName,
        metric -> {
          MetricAssert metricAssert = assertThat(metric);
          metricAssert.setStrict(strictMode);
          assertion.accept(metricAssert);
          metricAssert.strictCheck();
        });
    return this;
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
}
