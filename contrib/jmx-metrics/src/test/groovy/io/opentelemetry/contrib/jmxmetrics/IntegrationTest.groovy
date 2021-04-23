/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.jmxmetrics

import static org.junit.Assert.assertTrue

import java.time.Duration
import java.util.concurrent.TimeUnit

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.lifecycle.Startable
import org.testcontainers.utility.MountableFile
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class IntegrationTest extends Specification{

    @Shared
    def targets

    @Shared
    def targetContainers = []

    @Shared
    def jmxExtensionAppContainer

    @Shared
    def jmxExposedPort

    void configureContainers(String configName, int otlpPort, int prometheusPort, boolean configFromStdin) {
        def jarPath = System.getProperty("shadow.jar.path")

        def scriptName = "script.groovy"
        def scriptPath = ClassLoader.getSystemClassLoader().getResource(scriptName).path
        def configPath = ClassLoader.getSystemClassLoader().getResource(configName).path
        def network = Network.SHARED

        def jvmCommand = [
            "java",
            "-cp",
            "/app/OpenTelemetryJava.jar",
            "-Dotel.jmx.username=cassandra",
            "-Dotel.jmx.password=cassandra",
            "-Dotel.exporter.otlp.endpoint=host.testcontainers.internal:${otlpPort}",
            "io.opentelemetry.contrib.jmxmetrics.JmxMetrics",
            "-config",
        ]

        if (configFromStdin) {
            def cmd = jvmCommand.join(' ')
            jvmCommand = [
                "sh",
                "-c",
                "cat /app/${configName} | ${cmd} -",
            ]
        } else {
            jvmCommand.add("/app/${configName}")
        }

        if ("cassandra" in targets) {
            def dockerfile = ("FROM cassandra:3.11\nENV LOCAL_JMX=no\n"
                    + "RUN echo 'cassandra cassandra' > /etc/cassandra/jmxremote.password\n"
                    + "RUN chmod 0400 /etc/cassandra/jmxremote.password\n")
            targetContainers.add(
                    new GenericContainer<>(
                    new ImageFromDockerfile().withFileFromString( "Dockerfile", dockerfile)
                    ).withNetwork(network)
                    .withNetworkAliases("cassandra")
                    .withExposedPorts(7199)
                    .withStartupTimeout(Duration.ofSeconds(120))
                    .waitingFor(Wait.forListeningPort())
                    )
        }

        if (targets.any { it.contains("kafka") }) {
            def zookeeper = new GenericContainer<>("zookeeper:3.5")
                    .withNetwork(network)
                    .withNetworkAliases("zookeeper")
                    .withStartupTimeout(Duration.ofSeconds(120))
                    .waitingFor(Wait.forListeningPort())
            targetContainers.add(zookeeper)
            def kafka = new GenericContainer<>("bitnami/kafka:latest")
                    .withNetwork(network)
                    .withEnv([ "KAFKA_CFG_ZOOKEEPER_CONNECT" : "zookeeper:2181", "ALLOW_PLAINTEXT_LISTENER" : "yes", "JMX_PORT": "7199"])
                    .withNetworkAliases("kafka")
                    .withExposedPorts(7199)
                    .withStartupTimeout(Duration.ofSeconds(120))
                    .waitingFor(Wait.forListeningPort())
                    .dependsOn(zookeeper)
            targetContainers.add(kafka)
            if (targets.any {
                it in [
                    "kafka-consumer",
                    "kafka-producer"
                ]
            }) {
                def createTopics = new Startable() {
                            @Override
                            void start() {
                                kafka.execInContainer(
                                        "sh", "-c",
                                        "unset JMX_PORT; for i in `seq 3`; do kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test-topic-\$i; done"
                                        )
                            }

                            @Override
                            void stop() { }
                        }
                if ("kafka-consumer" in targets) {
                    def kafkaConsumer = new GenericContainer<>("bitnami/kafka:latest")
                            .withNetwork(network)
                            .withEnv([ "KAFKA_CFG_ZOOKEEPER_CONNECT" : "zookeeper:2181", "ALLOW_PLAINTEXT_LISTENER" : "yes", "JMX_PORT": "7199"])
                            .withNetworkAliases("kafka-consumer")
                            .withExposedPorts(7199)
                            .withCommand("kafka-console-consumer.sh", "--bootstrap-server", "kafka:9092",
                            "--whitelist", "test-topic-.*", "--max-messages", "100"
                            ).withStartupTimeout(Duration.ofSeconds(120))
                            .waitingFor(Wait.forListeningPort())
                            .dependsOn(kafka, createTopics)
                    targetContainers.add(kafkaConsumer)
                } else {
                    def producerPath = ClassLoader.getSystemClassLoader().getResource("target-systems/kafka-producer.sh").path
                    def kafkaProducer = new GenericContainer<>("bitnami/kafka:latest")
                            .withNetwork(network)
                            .withEnv([ "KAFKA_CFG_ZOOKEEPER_CONNECT" : "zookeeper:2181", "ALLOW_PLAINTEXT_LISTENER" : "yes", "JMX_PORT": "7199"])
                            .withNetworkAliases("kafka-producer")
                            .withExposedPorts(7199)
                            .withCopyFileToContainer(MountableFile.forHostPath(producerPath), "/usr/bin/kafka-producer.sh")
                            .withCommand( "kafka-producer.sh")
                            .withStartupTimeout(Duration.ofSeconds(120))
                            .waitingFor(Wait.forListeningPort())
                            .dependsOn(kafka, createTopics)
                    targetContainers.add(kafkaProducer)
                }
            }
        }

        targetContainers.each {
            it.start()
        }

        jmxExtensionAppContainer =
                new GenericContainer<>("openjdk:8u272-jre-slim")
                .withNetwork(network)
                .withCopyFileToContainer(MountableFile.forHostPath(jarPath), "/app/OpenTelemetryJava.jar")
                .withCopyFileToContainer(
                MountableFile.forHostPath(scriptPath), "/app/${scriptName}")
                .withCopyFileToContainer(
                MountableFile.forHostPath(configPath), "/app/${configName}")
                .withCommand(jvmCommand as String[])
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forLogMessage(".*Started GroovyRunner.*", 1))
                .dependsOn(targetContainers)
        if (prometheusPort != 0) {
            jmxExtensionAppContainer.withExposedPorts(prometheusPort)
        }
        jmxExtensionAppContainer.start()

        targetContainers.each {
            assertTrue(it.running)
        }
        assertTrue(jmxExtensionAppContainer.running)

        if (prometheusPort != 0) {
            jmxExposedPort = jmxExtensionAppContainer.getMappedPort(prometheusPort)
        }
    }
}

class OtlpIntegrationTest extends IntegrationTest  {

    @Shared
    def collector
    @Shared
    def collectorServer
    @Shared
    def otlpPort

    def setup() {
        // set up a collector per test to avoid noisy neighbor
        otlpPort = availablePort()
        collector = new Collector()
        collectorServer = ServerBuilder.forPort(otlpPort).addService(collector).build()
        collectorServer.start()
    }

    def cleanup() {
        collectorServer.shutdownNow()
        collectorServer.awaitTermination(5, TimeUnit.SECONDS)
    }

    static def availablePort() {
        def sock = new ServerSocket(0);
        def port = sock.getLocalPort()
        sock.close()
        return port
    }

    static final class Collector extends MetricsServiceGrpc.MetricsServiceImplBase {
        private final List<ResourceMetrics> receivedMetrics = new ArrayList<>()
        private final Object monitor = new Object()

        @Override
        void export(
                ExportMetricsServiceRequest request,
                StreamObserver<ExportMetricsServiceResponse> responseObserver) {
            synchronized (receivedMetrics) {
                receivedMetrics.addAll(request.resourceMetricsList)
            }
            synchronized (monitor) {
                monitor.notify()
            }
            responseObserver.onNext(ExportMetricsServiceResponse.newBuilder().build())
            responseObserver.onCompleted()
        }

        List<ResourceMetrics> getReceivedMetrics() {
            List<ResourceMetrics> received
            try {
                synchronized (monitor) {
                    monitor.wait(15000)
                }
            } catch (final InterruptedException e) {
                assertTrue(e.message, false)
            }

            synchronized (receivedMetrics) {
                received = new ArrayList<>(receivedMetrics)
                receivedMetrics.clear()
            }
            return received
        }
    }
}
