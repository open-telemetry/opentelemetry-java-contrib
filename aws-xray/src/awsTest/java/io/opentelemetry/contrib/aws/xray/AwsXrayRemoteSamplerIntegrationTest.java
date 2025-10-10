/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.xray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.awsxray.AwsXrayRemoteSampler;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

// We do not currently automate integration testing because it requires AWS credentials. If we were
// to set up AWS credentials in CI in the future, this test could be improved to use the AWS API
// to update sampling rules and assert rough ratios of sampling decisions. In the meantime, it
// expects you to update the rules through the dashboard to see the effect on the sampling decisions
// that are printed.
@Testcontainers(disabledWithoutDocker = true)
class AwsXrayRemoteSamplerIntegrationTest {

  private static final Logger logger =
      LoggerFactory.getLogger(AwsXrayRemoteSamplerIntegrationTest.class);

  @Container
  private static final GenericContainer<?> otelCollector =
      new GenericContainer<>(DockerImageName.parse("otel/opentelemetry-collector-contrib:0.136.0"))
          .withExposedPorts(13133, 2000)
          .waitingFor(Wait.forHttp("/").forPort(13133))
          .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("otel-collector")))
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("/otel-collector.yml"), "/etc/otel-collector.yml")
          .withCommand("--config /etc/otel-collector.yml --log-level DEBUG")
          .withEnv("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"))
          .withEnv("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"))
          .withEnv("AWS_REGION", System.getenv("AWS_REGION"));

  @Test
  void keepSampling() throws Exception {
    // Initialize two samplers to try to see centralized reservoir behavior.
    AwsXrayRemoteSampler sampler1 =
        AwsXrayRemoteSampler.newBuilder(Resource.getDefault())
            .setEndpoint("http://localhost:" + otelCollector.getMappedPort(2000))
            .setPollingInterval(Duration.ofSeconds(5))
            .build();

    AwsXrayRemoteSampler sampler2 =
        AwsXrayRemoteSampler.newBuilder(Resource.getDefault())
            .setEndpoint("http://localhost:" + otelCollector.getMappedPort(2000))
            .setPollingInterval(Duration.ofSeconds(5))
            .build();

    try {
      while (true) {
        logger.info("[Sampler 1] Sampling Decision: {}", doSample(sampler1).getDecision());
        logger.info("[Sampler 2] Sampling Decision: {}", doSample(sampler2).getDecision());
        Thread.sleep(500);
      }
    } finally {
      sampler1.close();
      sampler2.close();
    }
  }

  private static SamplingResult doSample(Sampler sampler) {
    return sampler.shouldSample(
        Context.root(),
        IdGenerator.random().generateTraceId(),
        "cat-service",
        SpanKind.SERVER,
        Attributes.empty(),
        Collections.emptyList());
  }
}
