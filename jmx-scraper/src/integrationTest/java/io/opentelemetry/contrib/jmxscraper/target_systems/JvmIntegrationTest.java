/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.TestAppContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcherGroup;
import java.nio.file.Path;
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
    AttributeMatcherGroup[] memoryAttributes =
        nameAttributeMatchers(
            "Code Cache",
            "PS Eden Space",
            "PS Old Gen",
            "Metaspace",
            "Compressed Class Space",
            "PS Survivor Space");
    AttributeMatcherGroup[] gcAlgorithmAttributes =
        nameAttributeMatchers("PS MarkSweep", "PS Scavenge");

    return MetricsVerifier.create()
        .add(
            "jvm.classes.loaded",
            metric ->
                metric
                    .hasDescription("number of loaded classes")
                    .hasUnit("1")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.gc.collections.count",
            metric ->
                metric
                    .hasDescription("total number of collections that have occurred")
                    .hasUnit("1")
                    .isCounter()
                    .hasDataPointsWithAttributes(gcAlgorithmAttributes))
        .add(
            "jvm.gc.collections.elapsed",
            metric ->
                metric
                    .hasDescription(
                        "the approximate accumulated collection elapsed time in milliseconds")
                    .hasUnit("ms")
                    .isCounter()
                    .hasDataPointsWithAttributes(gcAlgorithmAttributes))
        .add(
            "jvm.memory.heap.committed",
            metric ->
                metric
                    .hasDescription("current heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.heap.init",
            metric ->
                metric
                    .hasDescription("current heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.heap.max",
            metric ->
                metric
                    .hasDescription("current heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.heap.used",
            metric ->
                metric
                    .hasDescription("current heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.nonheap.committed",
            metric ->
                metric
                    .hasDescription("current non-heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.nonheap.init",
            metric ->
                metric
                    .hasDescription("current non-heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.nonheap.max",
            metric ->
                metric
                    .hasDescription("current non-heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.nonheap.used",
            metric ->
                metric
                    .hasDescription("current non-heap usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "jvm.memory.pool.committed",
            metric ->
                metric
                    .hasDescription("current memory pool usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithAttributes(memoryAttributes))
        .add(
            "jvm.memory.pool.init",
            metric ->
                metric
                    .hasDescription("current memory pool usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithAttributes(memoryAttributes))
        .add(
            "jvm.memory.pool.max",
            metric ->
                metric
                    .hasDescription("current memory pool usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithAttributes(memoryAttributes))
        .add(
            "jvm.memory.pool.used",
            metric ->
                metric
                    .hasDescription("current memory pool usage")
                    .hasUnit("by")
                    .isGauge()
                    .hasDataPointsWithAttributes(memoryAttributes))
        .add(
            "jvm.threads.count",
            metric ->
                metric
                    .hasDescription("number of threads")
                    .hasUnit("1")
                    .isGauge()
                    .hasDataPointsWithoutAttributes());
  }

  private static AttributeMatcherGroup[] nameAttributeMatchers(String... values) {
    AttributeMatcherGroup[] groups = new AttributeMatcherGroup[values.length];
    for (int i = 0; i < values.length; i++) {
      groups[i] = attributeGroup(attribute("name", values[i]));
    }
    return groups;
  }
}
