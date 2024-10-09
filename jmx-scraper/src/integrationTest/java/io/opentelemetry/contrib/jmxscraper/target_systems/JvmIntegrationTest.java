/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertGauge;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertTypedGauge;
import static io.opentelemetry.contrib.jmxscraper.target_systems.MetricAssertions.assertTypedSum;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.TestAppContainer;
import java.util.Arrays;
import java.util.List;
import org.testcontainers.containers.GenericContainer;

public class JvmIntegrationTest extends TargetSystemIntegrationTest {

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    // reusing test application for JVM metrics and custom yaml
    return new TestAppContainer().withJmxPort(jmxPort);
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
    return scraper.withTargetSystem("jvm");
  }

  @Override
  protected void verifyMetrics() {
    // those values depend on the JVM GC configured
    List<String> gcLabels =
        Arrays.asList(
            "Code Cache",
            "PS Eden Space",
            "PS Old Gen",
            "Metaspace",
            "Compressed Class Space",
            "PS Survivor Space");
    List<String> gcCollectionLabels = Arrays.asList("PS MarkSweep", "PS Scavenge");

    waitAndAssertMetrics(
        metric -> assertGauge(metric, "jvm.classes.loaded", "number of loaded classes", "1"),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.count",
                "total number of collections that have occurred",
                "1",
                gcCollectionLabels),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.elapsed",
                "the approximate accumulated collection elapsed time in milliseconds",
                "ms",
                gcCollectionLabels),
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
  }
}
