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
        metric -> assertGauge(metric, "jvm.classes.loaded", "number of loaded classes", "{class}"),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.count",
                "total number of collections that have occurred",
                "{collection}",
                Arrays.asList("ConcurrentMarkSweep", "ParNew")),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.elapsed",
                "the approximate accumulated collection elapsed time in milliseconds",
                "ms",
                Arrays.asList("ConcurrentMarkSweep", "ParNew")),
        metric -> assertGauge(metric, "jvm.memory.heap.committed", "current heap usage", "By"),
        metric -> assertGauge(metric, "jvm.memory.heap.init", "current heap usage", "By"),
        metric -> assertGauge(metric, "jvm.memory.heap.max", "current heap usage", "By"),
        metric -> assertGauge(metric, "jvm.memory.heap.used", "current heap usage", "By"),
        metric ->
            assertGauge(metric, "jvm.memory.nonheap.committed", "current non-heap usage", "By"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.init", "current non-heap usage", "By"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.max", "current non-heap usage", "By"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.used", "current non-heap usage", "By"),
        metric -> assertGauge(metric, "jvm.runtime.uptime", "uptime", "ms"),
        metric -> assertGauge(metric, "jvm.fd.open", "open file descriptors", "1"),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.committed", "current memory pool usage", "By", gcLabels),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.init", "current memory pool usage", "By", gcLabels),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.max", "current memory pool usage", "By", gcLabels),
        metric ->
            assertTypedGauge(
                metric, "jvm.memory.pool.used", "current memory pool usage", "By", gcLabels),
        metric -> assertGauge(metric, "jvm.threads.count", "number of threads", "{thread}"));
  }
}
