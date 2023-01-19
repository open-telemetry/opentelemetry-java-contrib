/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_COMPRESSED_CLASS_SPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_METASPACE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_COMMITTED;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MetaspaceMemoryCommittedMetricTest {

  @RegisterExtension JfrExtension jfrExtension = new JfrExtension();

  private void check(ThrowingConsumer<MetricData> attributeCheck) {
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.memory.committed")
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_COMMITTED)
                .satisfies(attributeCheck));
  }

  @Test
  void shouldHaveMemoryCommittedMetrics() {
    System.gc();
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints())
              .anyMatch(p -> p.getAttributes().equals(ATTR_COMPRESSED_CLASS_SPACE));
        });
    check(
        metricData -> {
          SumData<?> sumData = metricData.getLongSumData();
          assertThat(sumData.getPoints()).anyMatch(p -> p.getAttributes().equals(ATTR_METASPACE));
        });
  }
}
