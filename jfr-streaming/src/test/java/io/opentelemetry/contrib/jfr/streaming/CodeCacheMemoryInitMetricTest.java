/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming;

import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_CODE_CACHE;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_MEMORY_INIT;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_NAME_MEMORY_INIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CodeCacheMemoryInitMetricTest {

  @RegisterExtension
  JfrExtension jfrExtension =
      new JfrExtension(
          builder -> builder.disableAllFeatures().enableFeature(JfrFeature.MEMORY_POOL_METRICS));

  @Test
  void shouldHaveMemoryInitMetrics() {
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY_INIT)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_INIT)
                .hasUnit(BYTES)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      assertThat(sumData.getPoints())
                          .map(LongPointData.class::cast)
                          .anyMatch(
                              pointData ->
                                  pointData.getValue() > 0
                                      && pointData.getAttributes().equals(ATTR_CODE_CACHE));
                    }));
  }
}
