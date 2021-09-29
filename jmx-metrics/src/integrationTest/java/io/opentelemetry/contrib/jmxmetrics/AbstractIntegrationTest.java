/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.Testcontainers.exposeHostPorts;

import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

  private final boolean configFromStdin;
  private final String configName;

  private GenericContainer<?> scraper;
  private OtlpGrpcServer otlpServer;

  protected AbstractIntegrationTest(boolean configFromStdin, String configName) {
    this.configFromStdin = configFromStdin;
    this.configName = configName;
  }

  @BeforeAll
  void beforeAll() {
    otlpServer = new OtlpGrpcServer();
    otlpServer.start();
    exposeHostPorts(otlpServer.httpPort());

    String scraperJarPath = System.getProperty("shadow.jar.path");

    List<String> scraperCommand = new ArrayList<>();
    scraperCommand.add("java");
    scraperCommand.add("-cp");
    scraperCommand.add("/app/OpenTelemetryJava.jar");
    scraperCommand.add("-Dotel.jmx.username=cassandra");
    scraperCommand.add("-Dotel.jmx.password=cassandra");
    scraperCommand.add(
        "-Dotel.exporter.otlp.endpoint=http://host.testcontainers.internal:"
            + otlpServer.httpPort());
    scraperCommand.add("io.opentelemetry.contrib.jmxmetrics.JmxMetrics");
    scraperCommand.add("-config");

    if (configFromStdin) {
      String cmd = String.join(" ", scraperCommand);
      scraperCommand = Arrays.asList("sh", "-c", "cat /app/" + configName + " | " + cmd + " -");
    } else {
      scraperCommand.add("/app/" + configName);
    }

    scraper =
        new GenericContainer<>("openjdk:8u272-jre-slim")
            .withNetwork(Network.SHARED)
            .withCopyFileToContainer(
                MountableFile.forHostPath(scraperJarPath), "/app/OpenTelemetryJava.jar")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("script.groovy"), "/app/script.groovy")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource(configName), "/app/" + configName)
            .withCommand(scraperCommand.toArray(new String[0]))
            .withStartupTimeout(Duration.ofSeconds(120))
            .waitingFor(Wait.forLogMessage(".*Started GroovyRunner.*", 1));
    scraper.start();
  }

  @AfterAll
  void afterAll() {
    otlpServer.stop();
  }

  @BeforeEach
  void beforeEach() {
    otlpServer.reset();
  }

  protected void waitAndAssertMetrics(Consumer<Metric>... assertions) {
    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              List<Metric> metrics =
                  otlpServer.getMetrics().stream()
                      .map(ExportMetricsServiceRequest::getResourceMetricsList)
                      .flatMap(
                          rm ->
                              rm.stream()
                                  .map(ResourceMetrics::getInstrumentationLibraryMetricsList))
                      .flatMap(Collection::stream)
                      .filter(
                          ilm ->
                              ilm.getInstrumentationLibrary()
                                  .getName()
                                  .equals("io.opentelemetry.contrib.jmxmetrics"))
                      .flatMap(ilm -> ilm.getMetricsList().stream())
                      .collect(Collectors.toList());

              assertThat(metrics).isNotEmpty();

              for (Consumer<Metric> assertion : assertions) {
                assertThat(metrics).anySatisfy(assertion);
              }
            });
  }

  protected void assertGauge(Metric metric, String name, String description, String unit) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasGauge()).isTrue();
    assertThat(metric.getGauge().getDataPointsList())
        .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
  }

  protected void assertSum(Metric metric, String name, String description, String unit) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasSum()).isTrue();
    assertThat(metric.getSum().getDataPointsList())
        .satisfiesExactly(point -> assertThat(point.getAttributesList()).isEmpty());
  }

  protected void assertTypedGauge(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasGauge()).isTrue();
    assertTypedPoints(metric.getGauge().getDataPointsList(), types);
  }

  protected void assertTypedSum(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasSum()).isTrue();
    assertTypedPoints(metric.getSum().getDataPointsList(), types);
  }

  @SuppressWarnings("unchecked")
  private static void assertTypedPoints(List<NumberDataPoint> points, List<String> types) {
    assertThat(points)
        .satisfiesExactlyInAnyOrder(
            types.stream()
                .map(
                    type ->
                        (Consumer<NumberDataPoint>)
                            point ->
                                assertThat(point.getAttributesList())
                                    .singleElement()
                                    .satisfies(
                                        attribute -> {
                                          assertThat(attribute.getKey()).isEqualTo("name");
                                          assertThat(attribute.getValue().getStringValue())
                                              .isEqualTo(type);
                                        }))
                .toArray(Consumer[]::new));
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
