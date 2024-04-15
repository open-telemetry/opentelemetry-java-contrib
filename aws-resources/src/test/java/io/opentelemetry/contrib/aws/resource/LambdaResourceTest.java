/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_NAME;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_VERSION;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class LambdaResourceTest {
  @Test
  void shouldNotCreateResourceForNotLambda() {
    Attributes attributes = LambdaResource.buildResource(emptyMap()).getAttributes();
    assertThat(attributes).isEmpty();
  }

  @Test
  void shouldAddNonEmptyAttributes() {
    Resource resource =
        LambdaResource.buildResource(singletonMap("AWS_LAMBDA_FUNCTION_NAME", "my-function"));
    Attributes attributes = resource.getAttributes();

    assertThat(resource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_25_0);
    assertThat(attributes)
        .containsOnly(
            entry(CLOUD_PROVIDER, "aws"),
            entry(CLOUD_PLATFORM, "aws_lambda"),
            entry(FAAS_NAME, "my-function"));
  }

  @Test
  void shouldAddAllAttributes() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("AWS_REGION", "us-east-1");
    envVars.put("AWS_LAMBDA_FUNCTION_NAME", "my-function");
    envVars.put("AWS_LAMBDA_FUNCTION_VERSION", "1.2.3");

    Resource resource = LambdaResource.buildResource(envVars);
    Attributes attributes = resource.getAttributes();

    assertThat(resource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_25_0);
    assertThat(attributes)
        .containsOnly(
            entry(CLOUD_PROVIDER, "aws"),
            entry(CLOUD_PLATFORM, "aws_lambda"),
            entry(CLOUD_REGION, "us-east-1"),
            entry(FAAS_NAME, "my-function"),
            entry(FAAS_VERSION, "1.2.3"));
  }

  @Test
  void inServiceLoader() {
    // No practical way to test the attributes themselves so at least check the service loader picks
    // it up.
    assertThat(ServiceLoader.load(ResourceProvider.class))
        .anyMatch(LambdaResourceProvider.class::isInstance);
  }
}
