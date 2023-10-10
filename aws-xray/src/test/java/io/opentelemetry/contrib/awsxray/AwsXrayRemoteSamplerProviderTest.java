/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwsXrayRemoteSamplerProviderTest {

  @Test
  void serviceNameOnly() {
    Map<String, String> props = new HashMap<>();
    props.put("otel.traces.sampler", "xray");
    props.put("otel.service.name", "cat-service");
    props.put("otel.traces.exporter", "none");
    props.put("otel.metrics.exporter", "none");
    props.put("otel.logs.exporter", "none");
    try (SdkTracerProvider tracerProvider =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(() -> props)
            .build()
            .getOpenTelemetrySdk()
            .getSdkTracerProvider()) {
      // Inspect implementation details for simplicity, otherwise we'd probably need to make a
      // test HTTP server that records requests.
      assertThat(tracerProvider)
          .extracting("sharedState")
          .extracting("sampler")
          .isInstanceOfSatisfying(
              AwsXrayRemoteSampler.class,
              sampler -> {
                assertThat(sampler.getClient().getSamplingRulesEndpoint())
                    .isEqualTo("http://localhost:2000/GetSamplingRules");
                assertThat(sampler.getResource().getAttribute(ResourceAttributes.SERVICE_NAME))
                    .isEqualTo("cat-service");
              });
    }
  }

  @Test
  void setEndpoint() {
    Map<String, String> props = new HashMap<>();
    props.put("otel.traces.sampler", "xray");
    props.put("otel.traces.sampler.arg", "endpoint=http://localhost:3000");
    props.put("otel.service.name", "cat-service");
    props.put("otel.traces.exporter", "none");
    props.put("otel.metrics.exporter", "none");
    props.put("otel.logs.exporter", "none");
    try (SdkTracerProvider tracerProvider =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(() -> props)
            .build()
            .getOpenTelemetrySdk()
            .getSdkTracerProvider()) {
      // Inspect implementation details for simplicity, otherwise we'd probably need to make a
      // test HTTP server that records requests.
      assertThat(tracerProvider)
          .extracting("sharedState")
          .extracting("sampler")
          .isInstanceOfSatisfying(
              AwsXrayRemoteSampler.class,
              sampler -> {
                assertThat(sampler.getClient().getSamplingRulesEndpoint())
                    .isEqualTo("http://localhost:3000/GetSamplingRules");
                assertThat(sampler.getResource().getAttribute(ResourceAttributes.SERVICE_NAME))
                    .isEqualTo("cat-service");
              });
    }
  }
}
