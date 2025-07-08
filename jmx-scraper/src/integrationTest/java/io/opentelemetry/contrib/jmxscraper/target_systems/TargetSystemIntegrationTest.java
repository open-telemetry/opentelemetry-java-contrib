/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public abstract class TargetSystemIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(TargetSystemIntegrationTest.class);
  private static final Logger targetSystemLogger = LoggerFactory.getLogger("TargetSystemContainer");
  private static final Logger jmxScraperLogger = LoggerFactory.getLogger("JmxScraperContainer");
  private static final String TARGET_SYSTEM_NETWORK_ALIAS = "targetsystem";
  private static String otlpEndpoint;

  /**
   * Create target system container
   *
   * @param jmxPort JMX port target JVM should listen to
   * @return target system container
   */
  protected abstract GenericContainer<?> createTargetContainer(int jmxPort);

  private static Network network;
  private static OtlpGrpcServer otlpServer;
  private Collection<GenericContainer<?>> prerequisiteContainers;
  private GenericContainer<?> target;
  private JmxScraperContainer scraper;

  private static final String OTLP_HOST = "host.testcontainers.internal";

  // JMX communication only happens between container, and we don't have to use JMX
  // from host to container, we can use a fixed port.
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
    if (scraper != null && scraper.isRunning()) {
      scraper.stop();
    }

    if (target != null && target.isRunning()) {
      target.stop();
    }

    if (prerequisiteContainers != null) {
      prerequisiteContainers.forEach(
          container -> {
            if (container.isRunning()) {
              container.stop();
            }
          });
    }

    if (otlpServer != null) {
      otlpServer.reset();
    }
  }

  protected String scraperBaseImage() {
    return "openjdk:8u342-jre-slim";
  }

  @Test
  void endToEndTest(@TempDir Path tmpDir) {
    startContainers(tmpDir);
    verifyMetrics();
  }

  protected void startContainers(Path tmpDir) {
    prerequisiteContainers = createPrerequisiteContainers();

    target =
        createTargetContainer(JMX_PORT)
            .withLogConsumer(new Slf4jLogConsumer(targetSystemLogger))
            .withNetwork(network)
            .withNetworkAliases(TARGET_SYSTEM_NETWORK_ALIAS);

    // If there are any containers that must be started before target then initialize them.
    // Then make target depending on them, so it is started after dependencies
    for (GenericContainer<?> container : prerequisiteContainers) {
      container.withNetwork(network);
      target.dependsOn(container);
    }

    // Target container must be running before scraper container is customized.
    // It is necessary to allow interactions with the container, like file copying etc.
    target.start();

    // Create and initialize scraper container
    scraper =
        new JmxScraperContainer(otlpEndpoint, scraperBaseImage())
            // Since JVM metrics were be added to instrumentation, the default "auto" source
            // means that the definitions in instrumentation will be used, and thus tests will fail
            // due to metrics differences, adding an explicit "legacy" source is required to
            // continue
            // testing metrics defined in this project.
            .withTargetSystemSource("legacy")
            .withLogConsumer(new Slf4jLogConsumer(jmxScraperLogger))
            .withNetwork(network)
            .withRmiServiceUrl(TARGET_SYSTEM_NETWORK_ALIAS, JMX_PORT);

    scraper = customizeScraperContainer(scraper, target, tmpDir);
    scraper.start();
  }

  protected void verifyMetrics() {
    MetricsVerifier metricsVerifier = createMetricsVerifier();
    await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              List<ExportMetricsServiceRequest> receivedMetrics = otlpServer.getMetrics();
              assertThat(receivedMetrics).describedAs("No metric received").isNotEmpty();

              List<Metric> metrics =
                  receivedMetrics.stream()
                      .map(ExportMetricsServiceRequest::getResourceMetricsList)
                      .flatMap(rm -> rm.stream().map(ResourceMetrics::getScopeMetricsList))
                      .flatMap(Collection::stream)
                      .filter(
                          // TODO: disabling batch span exporter might help remove unwanted metrics
                          sm -> sm.getScope().getName().equals("io.opentelemetry.jmx"))
                      .flatMap(sm -> sm.getMetricsList().stream())
                      .collect(Collectors.toList());

              assertThat(metrics)
                  .describedAs("Metrics received but not suitable for JMX scraper")
                  .isNotEmpty();

              metricsVerifier.verify(metrics);
            });
  }

  protected abstract MetricsVerifier createMetricsVerifier();

  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper;
  }

  protected Collection<GenericContainer<?>> createPrerequisiteContainers() {
    return Collections.emptyList();
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

                      // verbose but helpful to diagnose what is received
                      logger.debug("receiving metrics {}", request);

                      metricRequests.add(request);
                      responseObserver.onNext(ExportMetricsServiceResponse.getDefaultInstance());
                      responseObserver.onCompleted();
                    }
                  })
              .build());
      sb.http(0);
    }
  }

  protected static String genericJmxJvmArguments(int port) {
    return "-Dcom.sun.management.jmxremote.local.only=false"
        + " -Dcom.sun.management.jmxremote.authenticate=false"
        + " -Dcom.sun.management.jmxremote.ssl=false"
        + " -Dcom.sun.management.jmxremote.port="
        + port
        + " -Dcom.sun.management.jmxremote.rmi.port="
        + port;
  }
}
