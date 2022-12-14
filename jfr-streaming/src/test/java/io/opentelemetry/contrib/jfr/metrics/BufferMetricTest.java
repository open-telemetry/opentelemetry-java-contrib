/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_BUFFERS;

import io.opentelemetry.api.common.Attributes;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BufferMetricTest extends AbstractMetricsTest {

  @Test
  void shouldHaveJfrLoadedClassesCountEvents() throws Exception {
    ByteBuffer buffer = ByteBuffer.allocateDirect(1000);
    buffer.put("test".getBytes(StandardCharsets.UTF_8));
    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.buffer.count")
                .hasDescription("Number of buffers in the pool")
                .hasUnit(UNIT_BUFFERS)
                .hasLongSumSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData -> {
                                      Assertions.assertTrue(pointData.getValue() > 0);
                                      Assertions.assertTrue(
                                          pointData
                                              .getAttributes()
                                              .equals(Attributes.of(ATTR_POOL, "direct")));
                                    }))),
        metric ->
            metric
                .hasName("process.runtime.jvm.buffer.limit")
                .hasDescription("Measure of total memory capacity of buffers")
                .hasUnit(BYTES)
                .hasLongSumSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData -> {
                                      Assertions.assertTrue(pointData.getValue() > 0);
                                      Assertions.assertTrue(
                                          pointData
                                              .getAttributes()
                                              .equals(Attributes.of(ATTR_POOL, "direct")));
                                    }))),
        metric ->
            metric
                .hasName("process.runtime.jvm.buffer.usage")
                .hasDescription("Measure of memory used by buffers")
                .hasUnit(BYTES)
                .hasLongSumSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData -> {
                                      Assertions.assertTrue(pointData.getValue() > 0);
                                      Assertions.assertTrue(
                                          pointData
                                              .getAttributes()
                                              .equals(Attributes.of(ATTR_POOL, "direct")));
                                    }))));
  }
}
