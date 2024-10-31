/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class HadoopIntegrationTest extends TargetSystemIntegrationTest {

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("bmedora/hadoop:2.9-base")
        .withEnv(
            "HADOOP_NAMENODE_OPTS",
            "-Dcom.sun.management.jmxremote.port="
                + jmxPort
                + " -Dcom.sun.management.jmxremote.rmi.port="
                + jmxPort
                + " -Dcom.sun.management.jmxremote.ssl=false"
                + " -Dcom.sun.management.jmxremote.authenticate=false"
                + " -Dcom.sun.management.jmxremote.local.only=false")
        .withStartupTimeout(Duration.ofMinutes(20))
//        .withExposedPorts(jmxPort, 10020, 19888, 50010, 50020, 50070, 50075, 50090, 8020, 8042, 8088, 9000)
        .withExposedPorts(jmxPort, 8004)
        .withCreateContainerCmdModifier(cmd -> cmd.withHostName("test-host"))
        .waitingFor(Wait.forListeningPort());
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
    return scraper.withTargetSystem("hadoop");
  }

  @Override
  protected void verifyMetrics() {
//    waitAndAssertMetrics(
//        metric ->
//            assertSumWithAttributes(
//                metric,
//                "hadoop.name_node.capacity.usage",
//                "The current used capacity across all data nodes reporting to the name node.",
//                "by",
//                attrs -> attrs.contains(entry("node_name", "test-host")))
//    );
    //    waitAndAssertMetrics(
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.range_slice.latency.50p",
    //                "Token range read request latency - 50th percentile",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.range_slice.latency.99p",
    //                "Token range read request latency - 99th percentile",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.range_slice.latency.max",
    //                "Maximum token range read request latency",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.read.latency.50p",
    //                "Standard read request latency - 50th percentile",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.read.latency.99p",
    //                "Standard read request latency - 99th percentile",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.read.latency.max",
    //                "Maximum standard read request latency",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.write.latency.50p",
    //                "Regular write request latency - 50th percentile",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.write.latency.99p",
    //                "Regular write request latency - 99th percentile",
    //                "us"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.client.request.write.latency.max",
    //                "Maximum regular write request latency",
    //                "us"),
    //        metric ->
    //            assertSum(
    //                metric,
    //                "cassandra.compaction.tasks.completed",
    //                "Number of completed compactions since server [re]start",
    //                "1"),
    //        metric ->
    //            assertGauge(
    //                metric,
    //                "cassandra.compaction.tasks.pending",
    //                "Estimated number of compactions remaining to perform",
    //                "1"),
    //        metric ->
    //            assertSum(
    //                metric,
    //                "cassandra.storage.load.count",
    //                "Size of the on disk data size this node manages",
    //                "by",
    //                /* isMonotonic= */ false),
    //        metric ->
    //            assertSum(
    //                metric,
    //                "cassandra.storage.total_hints.count",
    //                "Number of hint messages written to this node since [re]start",
    //                "1"),
    //        metric ->
    //            assertSum(
    //                metric,
    //                "cassandra.storage.total_hints.in_progress.count",
    //                "Number of hints attempting to be sent currently",
    //                "1",
    //                /* isMonotonic= */ false),
    //        metric ->
    //            assertSumWithAttributes(
    //                metric,
    //                "cassandra.client.request.count",
    //                "Number of requests by operation",
    //                "1",
    //                attrs -> attrs.containsOnly(entry("operation", "RangeSlice")),
    //                attrs -> attrs.containsOnly(entry("operation", "Read")),
    //                attrs -> attrs.containsOnly(entry("operation", "Write"))),
    //        metric ->
    //            assertSumWithAttributes(
    //                metric,
    //                "cassandra.client.request.error.count",
    //                "Number of request errors by operation",
    //                "1",
    //                getRequestErrorCountAttributes()));
  }
}
