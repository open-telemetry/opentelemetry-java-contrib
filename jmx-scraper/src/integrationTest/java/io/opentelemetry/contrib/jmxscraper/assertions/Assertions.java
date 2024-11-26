/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.assertions;

import io.opentelemetry.proto.metrics.v1.Metric;

public class Assertions extends org.assertj.core.api.Assertions {

  public static MetricAssert assertThat(Metric metric) {
    return new MetricAssert(metric);
  }
}
