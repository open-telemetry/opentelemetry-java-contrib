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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.MountableFile
import spock.lang.Shared
import spock.lang.Specification

class IntegrationTest extends Specification{

    @Shared
    def cassandraContainer

    @Shared
    def jmxExtensionAppContainer

    @Shared
    def jmxExposedPort

    void configureContainers(String configName, int exporterPort) {
        def jarPath = System.getProperty("shadow.jar.path")

        def scriptName = "script.groovy"
        def scriptPath = ClassLoader.getSystemClassLoader().getResource(scriptName).path

        def configPath = ClassLoader.getSystemClassLoader().getResource(configName).path

        def cassandraDockerfile = ("FROM cassandra:3.11\n"
                + "ENV LOCAL_JMX=no\n"
                + "RUN echo 'cassandra cassandra' > /etc/cassandra/jmxremote.password\n"
                + "RUN chmod 0400 /etc/cassandra/jmxremote.password\n")

        def network = Network.SHARED

        cassandraContainer =
                new GenericContainer<>(
                new ImageFromDockerfile().withFileFromString("Dockerfile", cassandraDockerfile))
                .withNetwork(network)
                .withNetworkAliases("cassandra")
                .withExposedPorts(7199)
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forListeningPort())
        cassandraContainer.start()

        jmxExtensionAppContainer =
                new GenericContainer<>("openjdk:7u111-jre-alpine")
                .withNetwork(network)
                .withCopyFileToContainer(MountableFile.forHostPath(jarPath), "/app/OpenTelemetryJava.jar")
                .withCopyFileToContainer(
                MountableFile.forHostPath(scriptPath), "/app/${scriptName}")
                .withCopyFileToContainer(
                MountableFile.forHostPath(configPath), "/app/${configName}")
                .withCommand("java -cp /app/OpenTelemetryJava.jar "
                + "-Dotel.jmx.username=cassandra "
                + "-Dotel.jmx.password=cassandra "
                + "io.opentelemetry.contrib.jmxmetrics.JmxMetrics "
                + "-config /app/${configName}")
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forLogMessage(".*Started GroovyRunner.*", 1))
                .dependsOn(cassandraContainer)
        if (exporterPort != 0) {
            jmxExtensionAppContainer.withExposedPorts(exporterPort)
        }
        jmxExtensionAppContainer.start()

        assertTrue(cassandraContainer.running)
        assertTrue(jmxExtensionAppContainer.running)

        if (exporterPort != 0) {
            jmxExposedPort = jmxExtensionAppContainer.getMappedPort(exporterPort)
        }
    }
}
