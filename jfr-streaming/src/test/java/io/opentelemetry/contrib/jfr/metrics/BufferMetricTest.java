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

  /**
   * This is a basic test that allocates some buffers and tests to make sure the resulting JFR event
   * was handled and turned into the expected metrics.
   *
   * <p>This test handles all 3 buffer related metrics defined in the OpenTelemetry Java runtime
   * Semantic Conventions.
   *
   * <p>Currenly JFR only has support for the "direct" buffer pool. The "mapped" and "mapped -
   * 'non-volatile memory'" pools do not have corresponding JFR events. TODO: In the future events
   * should be added for these, if there is to capture their data in the
   * process.runtime.jvm.buffer.count metric.
   */
  @Test
  void shouldHaveJfrLoadedClassesCountEvents() throws Exception {
    ByteBuffer buffer = ByteBuffer.allocateDirect(10000);
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
