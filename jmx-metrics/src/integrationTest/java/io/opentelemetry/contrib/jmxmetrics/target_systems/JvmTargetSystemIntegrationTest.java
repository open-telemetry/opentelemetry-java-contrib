/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

class JvmTargetSystemIntegrationTest extends AbstractIntegrationTest {

  JvmTargetSystemIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/jvm.properties");
  }

  @Container GenericContainer<?> cassandra = cassandraContainer();

  @Test
  void endToEnd() {
    List<String> gcLabels =
        Arrays.asList(
            "Code Cache",
            "Par Eden Space",
            "CMS Old Gen",
            "Compressed Class Space",
            "Metaspace",
            "Par Survivor Space");
    waitAndAssertMetrics(
        metric -> assertGauge(metric, "jvm.classes.loaded", "number of loaded classes", "1"),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.count",
                "total number of collections that have occurred",
                "1",
                Arrays.asList("ConcurrentMarkSweep", "ParNew")),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.elapsed",
                "the approximate accumulated collection elapsed time in milliseconds",
                "ms",
                Arrays.asList("ConcurrentMarkSweep", "ParNew")),
        metric -> assertGauge(metric, "jvm.memory.heap.committed", "current heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.heap.init", "current heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.heap.max", "current heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.heap.used", "current heap usage", "by"),
        metric ->
            assertGauge(metric, "jvm.memory.nonheap.committed", "current non-heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.init", "current non-heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.max", "current non-heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.used", "current non-heap usage", "by"),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.committed", "current memory pool usage", "by", gcLabels),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.init", "current memory pool usage", "by", gcLabels),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.max", "current memory pool usage", "by", gcLabels),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.used", "current memory pool usage", "by", gcLabels),
        metric -> assertGauge(metric, "jvm.threads.count", "number of threads", "1"));

    cassandra.stop();

    waitAndAssertNoMetrics();
  }
}
