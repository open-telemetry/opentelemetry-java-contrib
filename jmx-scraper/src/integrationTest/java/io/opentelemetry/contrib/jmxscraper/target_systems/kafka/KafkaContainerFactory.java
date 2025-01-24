/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems.kafka;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class KafkaContainerFactory {
  private static final int KAFKA_PORT = 9092;
  private static final String KAFKA_BROKER = "kafka:" + KAFKA_PORT;
  private static final String KAFKA_DOCKER_IMAGE = "bitnami/kafka:2.8.1";

  private KafkaContainerFactory() {}

  public static GenericContainer<?> createZookeeperContainer() {
    return new GenericContainer<>("zookeeper:3.5")
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forListeningPort());
  }

  public static GenericContainer<?> createKafkaContainer() {
    return new GenericContainer<>(KAFKA_DOCKER_IMAGE)
        .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
        .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes") // Removed in 3.5.1
        .withStartupTimeout(Duration.ofMinutes(2))
        .withExposedPorts(KAFKA_PORT)
        //        .waitingFor(Wait.forListeningPorts(KAFKA_PORT));
        .waitingFor(
            Wait.forLogMessage(".*KafkaServer.*started \\(kafka.server.KafkaServer\\).*", 1));
  }

  public static GenericContainer<?> createKafkaProducerContainer() {
    return new GenericContainer<>(KAFKA_DOCKER_IMAGE)
        //        .withCopyFileToContainer(
        //            MountableFile.forClasspathResource("kafka-producer.sh"),
        //            "/usr/bin/kafka-producer.sh")
        //        .withCommand("/usr/bin/kafka-producer.sh")
        .withCommand(
            "sh",
            "-c",
            "echo 'Sending messages to test-topic-1'; "
                + "i=1; while true; do echo \"Message $i\"; sleep .25; i=$((i+1)); done | /opt/bitnami/kafka/bin/kafka-console-producer.sh --bootstrap-server "
                + KAFKA_BROKER
                + " --topic test-topic-1;")
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forLogMessage(".*Welcome to the Bitnami kafka container.*", 1));
  }

  public static GenericContainer<?> createKafkaConsumerContainer() {
    return new GenericContainer<>(KAFKA_DOCKER_IMAGE)
        .withCommand(
            "kafka-console-consumer.sh",
            "--bootstrap-server",
            KAFKA_BROKER,
            "--whitelist",
            "test-topic-.*",
            "--max-messages",
            "100")
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forListeningPort());
  }
}
