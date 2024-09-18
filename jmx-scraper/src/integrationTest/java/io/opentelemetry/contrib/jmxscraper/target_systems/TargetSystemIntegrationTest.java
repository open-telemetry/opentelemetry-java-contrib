/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static org.assertj.core.api.Assertions.assertThat;

import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.contrib.jmxscraper.client.JmxRemoteClient;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import javax.management.remote.JMXConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public abstract class TargetSystemIntegrationTest {

  private static final Logger logger = LoggerFactory.getLogger(TargetSystemIntegrationTest.class);
  private static String otlpEndpoint;

  /**
   * Create target system container
   *
   * @param jmxPort JMX port target JVM should listen to
   * @return target system container
   */
  protected abstract GenericContainer<?> createTargetContainer(int jmxPort);

  protected abstract String getTargetSystem();

  private static Network network;
  private static OtlpGrpcServer otlpServer;
  private GenericContainer<?> target;
  private GenericContainer<?> scraper;

  private static final String OTLP_HOST = "host.testcontainers.internal";
  private static final int JMX_PORT = 9999;

  @BeforeAll
  static void beforeAll() {
    network = Network.newNetwork();
    otlpServer = new OtlpGrpcServer();
    otlpServer.start();
    Testcontainers.exposeHostPorts(otlpServer.httpPort());
    otlpEndpoint = "http://" + OTLP_HOST + ":" + otlpServer.httpPort();
  }

  @AfterAll
  static void afterAll() {
    network.close();
    try {
      otlpServer.stop().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void afterEach() {
    if (target != null && target.isRunning()) {
      target.stop();
    }
    if (scraper != null && scraper.isRunning()) {
      scraper.stop();
    }
    if (otlpServer != null) {
      otlpServer.reset();
    }
  }

  @Test
  void endToEndTest() {

    target =
        createTargetContainer(JMX_PORT)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withNetwork(network)
            .withExposedPorts(JMX_PORT)
            .withNetworkAliases("target_system");
    target.start();

    String targetHost = target.getHost();
    Integer targetPort = target.getMappedPort(JMX_PORT);
    logger.info(
        "Target system started, JMX port: {} mapped to {}:{}", JMX_PORT, targetHost, targetPort);

    scraper =
        createScraperContainer(otlpEndpoint, getTargetSystem(), null, "target_system", JMX_PORT);
    logger.info(
        "starting scraper with command: {}", String.join(" ", scraper.getCommandParts()));

    scraper.start();

    // TODO : wait for metrics to be sent and add assertions on what is being captured
    // for now we just test that we can connect to remote JMX using our client.
    try (JMXConnector connector = JmxRemoteClient.createNew(targetHost, targetPort).connect()) {
      assertThat(connector.getMBeanServerConnection()).isNotNull();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // TODO: replace with real assertions
    assertThat(otlpServer.getMetrics()).isEmpty();
  }

  protected GenericContainer<?> createScraperContainer(
      String otlpEndpoint,
      String targetSystem,
      String customYaml,
      String targetHost,
      int targetPort) {

    String scraperJarPath = System.getProperty("shadow.jar.path");
    assertThat(scraperJarPath).isNotNull();

    // TODO: adding a way to provide 'host:port' syntax would make this easier for common use
    String url =
        String.format(
            Locale.getDefault(),
            "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi",
            targetHost,
            targetPort);

    // for now only configure through JVM args
    List<String> arguments =
        new ArrayList<>(
            Arrays.asList(
                "java",
                "-Dotel.exporter.otlp.endpoint=" + otlpEndpoint,
                "-Dotel.jmx.target.system=" + targetSystem,
                "-Dotel.jmx.interval.milliseconds=1000",
                "-Dotel.jmx.service.url=" + url,
                "-jar",
                "/scraper.jar"));

    GenericContainer<?> scraper =
        new GenericContainer<>("openjdk:8u272-jre-slim")
            .withNetwork(network)
            .withCopyFileToContainer(MountableFile.forHostPath(scraperJarPath), "/scraper.jar")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .waitingFor(
                Wait.forLogMessage(".*JMX scraping started.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(10)));

    if (customYaml != null) {
      arguments.add("-Dotel.jmx.config=/custom.yaml");
      scraper.withCopyFileToContainer(
          MountableFile.forClasspathResource(customYaml), "/custom.yaml");
    }

    scraper.withCommand(arguments.toArray(new String[0]));

    return scraper;
  }

  private static class OtlpGrpcServer extends ServerExtension {

    private final BlockingQueue<ExportMetricsServiceRequest> metricRequests =
        new LinkedBlockingDeque<>();

    List<ExportMetricsServiceRequest> getMetrics() {
      return new ArrayList<>(metricRequests);
    }

    void reset() {
      metricRequests.clear();
    }

    @Override
    protected void configure(ServerBuilder sb) {
      sb.service(
          GrpcService.builder()
              .addService(
                  new MetricsServiceGrpc.MetricsServiceImplBase() {
                    @Override
                    public void export(
                        ExportMetricsServiceRequest request,
                        StreamObserver<ExportMetricsServiceResponse> responseObserver) {
                      metricRequests.add(request);
                      responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
                      responseObserver.onCompleted();
                    }
                  })
              .build());
      sb.http(0);
    }
  }
}
