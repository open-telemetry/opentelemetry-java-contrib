/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.TestAppContainer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class JvmIntegrationTest extends TargetSystemIntegrationTest {

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    // reusing test application for JVM metrics and custom yaml
    //noinspection resource
    return new TestAppContainer()
        .withJmxPort(jmxPort)
        .withExposedPorts(jmxPort)
        .waitingFor(Wait.forListeningPorts(jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("jvm");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
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

    MetricsVerifier metricsVerifier =
        MetricsVerifier.create()
            .assertGauge("jvm.classes.loaded", "number of loaded classes", "1")
            .assertTypedCounter(
                "jvm.gc.collections.count",
                "total number of collections that have occurred",
                "1",
                gcCollectionLabels)
            .register(
                "jvm.gc.collections.elapsed",
                metric ->
                    metric
                        .hasDescription(
                            "the approximate accumulated collection elapsed time in milliseconds")
                        .hasUnit("ms")
                        .isCounter()
                        .hasTypedDataPoints(gcCollectionLabels))
            .assertGauge("jvm.memory.heap.committed", "current heap usage", "by")
            .assertGauge("jvm.memory.heap.init", "current heap usage", "by")
            .assertGauge("jvm.memory.heap.max", "current heap usage", "by")
            .assertGauge("jvm.memory.heap.used", "current heap usage", "by")
            .assertGauge("jvm.memory.nonheap.committed", "current non-heap usage", "by")
            .assertGauge("jvm.memory.nonheap.init", "current non-heap usage", "by")
            .assertGauge("jvm.memory.nonheap.max", "current non-heap usage", "by")
            .assertGauge("jvm.memory.nonheap.used", "current non-heap usage", "by")
            .assertTypedGauge(
                "jvm.memory.pool.committed", "current memory pool usage", "by", gcLabels)
            .assertTypedGauge("jvm.memory.pool.init", "current memory pool usage", "by", gcLabels)
            .assertTypedGauge("jvm.memory.pool.max", "current memory pool usage", "by", gcLabels)
            .assertTypedGauge("jvm.memory.pool.used", "current memory pool usage", "by", gcLabels)
            .assertGauge("jvm.threads.count", "number of threads", "1");

    return metricsVerifier;
  }
}
