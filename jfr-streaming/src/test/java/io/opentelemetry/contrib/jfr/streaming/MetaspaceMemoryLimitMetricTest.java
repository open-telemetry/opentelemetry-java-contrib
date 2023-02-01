/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming;

import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_COMPRESSED_CLASS_SPACE;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_MEMORY_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MetaspaceMemoryLimitMetricTest {

  @RegisterExtension
  JfrExtension jfrExtension =
      new JfrExtension(
          builder -> builder.disableAllFeatures().enableFeature(JfrFeature.MEMORY_POOL_METRICS));

  @Test
  void shouldHaveMemoryLimitMetrics() {
    System.gc();
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints())
              .anyMatch(p -> p.getAttributes().equals(ATTR_COMPRESSED_CLASS_SPACE));
        });
  }

  private void check(ThrowingConsumer<MetricData> attributeCheck) {
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.memory.limit")
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_LIMIT)
                .satisfies(attributeCheck));
  }
}
