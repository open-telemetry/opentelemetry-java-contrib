/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import com.linecorp.armeria.internal.common.util.PortUtil;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
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

public abstract class TargetSystemIntegrationTest {
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
  private GenericContainer<?> target;
  private JmxScraperContainer scraper;

  private static final String OTLP_HOST = "host.testcontainers.internal";

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
    int jmxPort = PortUtil.unusedTcpPort();

    target =
        createTargetContainer(jmxPort)
            .withLogConsumer(new Slf4jLogConsumer(targetSystemLogger))
            .withNetwork(network)
            .withNetworkAliases(TARGET_SYSTEM_NETWORK_ALIAS);
    target.start();

    scraper =
        new JmxScraperContainer(otlpEndpoint)
            .withLogConsumer(new Slf4jLogConsumer(jmxScraperLogger))
            .withNetwork(network)
            .withService(TARGET_SYSTEM_NETWORK_ALIAS, jmxPort);

    scraper = customizeScraperContainer(scraper);
    scraper.start();

    verifyMetrics(otlpServer.getMetrics());
  }

  protected abstract void verifyMetrics(List<ExportMetricsServiceRequest> metrics);

  protected JmxScraperContainer customizeScraperContainer(JmxScraperContainer scraper) {
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
