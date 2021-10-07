/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;

class MultiObjTest extends AbstractIntegrationTest {
  MultiObjTest() {
    super(false, "multi_obj_config.properties");
  }

  @Container
  GenericContainer<?> cassandra =
      new GenericContainer<>("cassandra:3.11")
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("cassandra/jmxremote.password", 0400),
              "/etc/cassandra/jmxremote.password")
          .withNetworkAliases("cassandra")
          .withExposedPorts(7199)
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric -> {
          assertThat(metric.getName()).isEqualTo("cassandra.current_tasks");
          assertThat(metric.getDescription())
              .isEqualTo("Number of tasks in queue with the given task status.");
          assertThat(metric.getUnit()).isEqualTo("1");
          assertThat(metric.hasGauge()).isTrue();
          assertThat(metric.getGauge().getDataPointsList())
              .satisfiesExactlyInAnyOrder(
                  point ->
                      assertThat(point.getAttributesList())
                          .satisfiesExactlyInAnyOrder(
                              attribute -> {
                                assertThat(attribute.getKey()).isEqualTo("stage_name");
                                assertThat(attribute.getValue().getStringValue()).isNotEmpty();
                              },
                              attribute -> {
                                assertThat(attribute.getKey()).isEqualTo("task_status");
                                assertThat(attribute.getValue().getStringValue()).isEqualTo("PendingTasks");
                              }),
                  point ->
                      assertThat(point.getAttributesList())
                          .satisfiesExactlyInAnyOrder(
                              attribute -> {
                                assertThat(attribute.getKey()).isEqualTo("stage_name");
                                assertThat(attribute.getValue().getStringValue()).isNotEmpty();
                              },
                              attribute -> {
                                assertThat(attribute.getKey()).isEqualTo("task_status");
                                assertThat(attribute.getValue().getStringValue()).isEqualTo("ActiveTasks");
                              }));
        });
  }
}
