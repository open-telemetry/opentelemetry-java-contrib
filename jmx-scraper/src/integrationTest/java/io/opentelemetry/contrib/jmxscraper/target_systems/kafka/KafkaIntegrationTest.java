/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems.kafka;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static io.opentelemetry.contrib.jmxscraper.target_systems.kafka.KafkaContainerFactory.createKafkaContainer;
import static io.opentelemetry.contrib.jmxscraper.target_systems.kafka.KafkaContainerFactory.createZookeeperContainer;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcherGroup;
import io.opentelemetry.contrib.jmxscraper.target_systems.MetricsVerifier;
import io.opentelemetry.contrib.jmxscraper.target_systems.TargetSystemIntegrationTest;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class KafkaIntegrationTest extends TargetSystemIntegrationTest {
  @Override
  protected Collection<GenericContainer<?>> createPrerequisiteContainers() {
    GenericContainer<?> zookeeper =
        createZookeeperContainer()
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("zookeeper")))
            .withNetworkAliases("zookeeper");

    return Collections.singletonList(zookeeper);
  }

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return createKafkaContainer().withEnv("JMX_PORT", Integer.toString(jmxPort));
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("kafka");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {
    AttributeMatcherGroup[] requestTypes = {
      // attribute values are changed from lowercase to PascalCase
      // because it is impossible to make them lowercase with YAML config
      // TODO: What about const value attributes defined in YAML that are lowercase?
      attributeGroup(attribute("type", "Produce")),
      attributeGroup(attribute("type", "FetchFollower")),
      attributeGroup(attribute("type", "FetchConsumer"))
    };

    return MetricsVerifier.create()
        .add(
            "kafka.message.count",
            metric ->
                metric
                    .hasDescription("The number of messages received by the broker")
                    .hasUnit("{message}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.request.count",
            metric ->
                metric
                    .hasDescription("The number of requests received by the broker")
                    .hasUnit("{request}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("type", "produce")),
                        attributeGroup(attribute("type", "fetch"))))
        .add(
            "kafka.request.failed",
            metric ->
                metric
                    .hasDescription("The number of requests to the broker resulting in a failure")
                    .hasUnit("{request}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("type", "produce")),
                        attributeGroup(attribute("type", "fetch"))))
        .add(
            "kafka.network.io",
            metric ->
                metric
                    .hasDescription("The bytes received or sent by the broker")
                    .hasUnit("By")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("direction", "in")),
                        attributeGroup(attribute("direction", "out"))))
        .add(
            "kafka.purgatory.size",
            metric ->
                metric
                    .hasDescription("The number of requests waiting in purgatory")
                    .hasUnit("{request}")
                    .isUpDownCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("type", "Produce")),
                        attributeGroup(attribute("type", "Fetch"))))
        .add(
            "kafka.request.time.total",
            metric ->
                metric
                    .hasDescription("The total time the broker has taken to service requests")
                    .hasUnit("ms")
                    .isCounter()
                    .hasDataPointsWithAttributes(requestTypes))
        .add(
            "kafka.request.time.50p",
            metric ->
                metric
                    .hasDescription(
                        "The 50th percentile time the broker has taken to service requests")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithAttributes(requestTypes))
        .add(
            "kafka.request.time.99p",
            metric ->
                metric
                    .hasDescription(
                        "The 99th percentile time the broker has taken to service requests")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithAttributes(requestTypes))
        .add(
            "kafka.request.time.avg",
            metric ->
                metric
                    .hasDescription("The average time the broker has taken to service requests")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithAttributes(requestTypes))
        .add(
            "kafka.request.queue",
            metric ->
                metric
                    .hasDescription("Size of the request queue")
                    .hasUnit("{request}")
                    .isUpDownCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.partition.count",
            metric ->
                metric
                    .hasDescription("The number of partitions on the broker")
                    .hasUnit("{partition}")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.partition.offline",
            metric ->
                metric
                    .hasDescription("The number of partitions offline")
                    .hasUnit("{partition}")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.partition.under_replicated",
            metric ->
                metric
                    .hasDescription("The number of under replicated partitions")
                    .hasUnit("{partition}")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.isr.operation.count",
            metric ->
                metric
                    .hasDescription("The number of in-sync replica shrink and expand operations")
                    .hasUnit("{operation}")
                    .isCounter()
                    .hasDataPointsWithAttributes(
                        attributeGroup(attribute("operation", "shrink")),
                        attributeGroup(attribute("operation", "expand"))))
        .add(
            "kafka.controller.active.count",
            metric ->
                metric
                    .hasDescription("The number of controllers active on the broker") // CHANGED
                    .hasUnit("{controller}")
                    .isUpDownCounter() // CHANGED
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.leader.election.count", // CHANGED from "kafka.leader.election.rate"
            metric ->
                metric
                    .hasDescription("The leader election count")
                    .hasUnit("{election}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.leader.election.unclean.count", // CHANGED from "kafka.unclean.election.rate"
            metric ->
                metric
                    .hasDescription(
                        "Unclean leader election count - increasing indicates broker failures") // CHANGED
                    .hasUnit("{election}")
                    .isCounter()
                    .hasDataPointsWithoutAttributes())
        .add(
            "kafka.lag.max", // CHANGED from "kafka.max.lag"
            metric ->
                metric
                    .hasDescription(
                        "The max lag in messages between follower and leader replicas") // CHANGED
                    .hasUnit("{message}")
                    .isGauge()
                    .hasDataPointsWithoutAttributes())

    // TODO: Find out how to force Kafka to generate these metrics
    //        .add(
    //            "kafka.logs.flush.count",
    //            metric ->
    //                metric
    //                    .hasDescription("Log flush count")
    //                    .hasUnit("{flush}")
    //                    .isCounter()
    //                    .hasDataPointsWithoutAttributes())
    //        .add(
    //            "kafka.logs.flush.time.median",
    //            metric ->
    //                metric
    //                    .hasDescription("Log flush time - 50th percentile")
    //                    .hasUnit("ms")
    //                    .isGauge()
    //                    .hasDataPointsWithoutAttributes())
    //        .add(
    //            "kafka.logs.flush.time.99p",
    //            metric ->
    //                metric
    //                    .hasDescription("Log flush time - 99th percentile")
    //                    .hasUnit("ms")
    //                    .isGauge()
    //                    .hasDataPointsWithoutAttributes())
    ;
  }
}
