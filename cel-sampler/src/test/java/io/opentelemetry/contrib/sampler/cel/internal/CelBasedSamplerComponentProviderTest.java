/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel.internal;

import static io.opentelemetry.sdk.trace.samplers.SamplingResult.recordAndSample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import dev.cel.common.CelValidationException;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.sampler.cel.CelBasedSampler;
import io.opentelemetry.contrib.sampler.cel.CelBasedSamplerBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.HttpAttributes;
import java.io.InputStream;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CelBasedSamplerComponentProviderTest {

  private static final CelBasedSamplerComponentProvider PROVIDER =
      new CelBasedSamplerComponentProvider();

  private static InputStream loadResource(String resourcePath) {
    return CelBasedSamplerComponentProviderTest.class.getResourceAsStream("/" + resourcePath);
  }

  @Test
  void shouldCreateSamplerFromYamlConfiguration() {
    // Load YAML configuration from resource file
    InputStream configStream = loadResource("cel-sampler-config.yaml");

    OpenTelemetrySdk openTelemetrySdk = DeclarativeConfiguration.parseAndCreate(configStream);
    Sampler sampler = openTelemetrySdk.getSdkTracerProvider().getSampler();

    // Create expected sampler for comparison
    CelBasedSampler expectedSampler;
    try {
      expectedSampler =
          CelBasedSampler.builder(Sampler.alwaysOn())
              .drop(
                  "\"example.com\" in attribute[\"http.response.header.host\"] && attribute[\"http.response.status_code\"] == 200")
              .drop("spanKind == \"SERVER\" && attribute[\"url.path\"].matches(\"/actuator.*\")")
              .recordAndSample(
                  "spanKind == \"SERVER\" && attribute[\"url.path\"].matches(\"/actuator.*\")")
              .build();
    } catch (CelValidationException e) {
      throw new RuntimeException("Failed to create expected sampler", e);
    }

    // Verify that the sampler behaves correctly with actual sampling calls
    verifySamplerBehavior(sampler);

    Sampler expectedParentBasedSampler = Sampler.parentBasedBuilder(expectedSampler).build();
    // Verify that the sampler configuration matches the expected configuration
    verifySamplerEquality(sampler, expectedParentBasedSampler);
  }

  static void verifySamplerEqualityForValidTests(CelBasedSampler actual, CelBasedSampler expected) {
    // Compare descriptions which contain the configuration details
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());

    // Test with various scenarios to ensure behavior equivalence
    Context context = Context.root();
    String traceId = IdGenerator.random().generateTraceId();

    // Test scenario 1: CLIENT span kind for drop test
    Attributes clientAttrs = Attributes.builder().put("url.path", "/test").build();

    assertThat(
            actual.shouldSample(
                context,
                traceId,
                "GET /test",
                SpanKind.CLIENT,
                clientAttrs,
                Collections.emptyList()))
        .isEqualTo(
            expected.shouldSample(
                context,
                traceId,
                "GET /test",
                SpanKind.CLIENT,
                clientAttrs,
                Collections.emptyList()));

    // Test scenario 2: SERVER span with specific path for record test
    Attributes serverAttrs = Attributes.builder().put("url.path", "/v1/user").build();

    assertThat(
            actual.shouldSample(
                context,
                traceId,
                "GET /v1/user",
                SpanKind.SERVER,
                serverAttrs,
                Collections.emptyList()))
        .isEqualTo(
            expected.shouldSample(
                context,
                traceId,
                "GET /v1/user",
                SpanKind.SERVER,
                serverAttrs,
                Collections.emptyList()));

    // Test scenario 3: Multiple expressions test with GET method
    Attributes multiAttrs =
        Attributes.builder().put("http.request.method", "GET").put("url.path", "/foo/bar").build();

    assertThat(
            actual.shouldSample(
                context,
                traceId,
                "GET /foo/bar",
                SpanKind.SERVER,
                multiAttrs,
                Collections.emptyList()))
        .isEqualTo(
            expected.shouldSample(
                context,
                traceId,
                "GET /foo/bar",
                SpanKind.SERVER,
                multiAttrs,
                Collections.emptyList()));
  }

  static void verifySamplerEquality(Sampler actual, Sampler expected) {
    // Compare descriptions which contain the configuration details
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());

    // Test with various scenarios to ensure behavior equivalence
    Context context = Context.root();
    String traceId = IdGenerator.random().generateTraceId();

    // Test scenario 1: Should drop due to host header and status code
    Attributes attrs1 =
        Attributes.builder()
            .put(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200)
            .put(HttpAttributes.HTTP_RESPONSE_HEADER.getAttributeKey("host"), "example.com")
            .build();

    assertThat(
            actual.shouldSample(
                context, traceId, "GET /test", SpanKind.SERVER, attrs1, Collections.emptyList()))
        .isEqualTo(
            expected.shouldSample(
                context, traceId, "GET /test", SpanKind.SERVER, attrs1, Collections.emptyList()));

    // Test scenario 2: Should drop due to actuator path
    Attributes attrs2 = Attributes.builder().put("url.path", "/actuator/health").build();

    assertThat(
            actual.shouldSample(
                context,
                traceId,
                "GET /actuator/health",
                SpanKind.SERVER,
                attrs2,
                Collections.emptyList()))
        .isEqualTo(
            expected.shouldSample(
                context,
                traceId,
                "GET /actuator/health",
                SpanKind.SERVER,
                attrs2,
                Collections.emptyList()));

    // Test scenario 3: Should record and sample for regular paths
    Attributes attrs3 = Attributes.builder().put("url.path", "/v1/users").build();

    assertThat(
            actual.shouldSample(
                context,
                traceId,
                "GET /v1/users",
                SpanKind.SERVER,
                attrs3,
                Collections.emptyList()))
        .isEqualTo(
            expected.shouldSample(
                context,
                traceId,
                "GET /v1/users",
                SpanKind.SERVER,
                attrs3,
                Collections.emptyList()));
  }

  static void verifySamplerBehavior(Sampler sampler) {
    // SERVER span matching host and status code should be dropped
    assertThat(
            sampler.shouldSample(
                Context.root(),
                IdGenerator.random().generateTraceId(),
                "GET /v1/users",
                SpanKind.SERVER,
                Attributes.builder()
                    .put(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200)
                    .put(HttpAttributes.HTTP_RESPONSE_HEADER.getAttributeKey("host"), "example.com")
                    .build(),
                Collections.emptyList()))
        .isEqualTo(SamplingResult.drop());

    // SERVER span to /actuator.* path should be dropped (first matching expression)
    assertThat(
            sampler.shouldSample(
                Context.root(),
                IdGenerator.random().generateTraceId(),
                "GET /actuator/health",
                SpanKind.SERVER,
                Attributes.builder().put("url.path", "/actuator/health").build(),
                Collections.emptyList()))
        .isEqualTo(SamplingResult.drop());

    // SERVER span to other path should be recorded and sampled
    assertThat(
            sampler.shouldSample(
                Context.root(),
                IdGenerator.random().generateTraceId(),
                "GET /v1/users",
                SpanKind.SERVER,
                Attributes.builder().put("url.path", "/v1/users").build(),
                Collections.emptyList()))
        .isEqualTo(recordAndSample());
  }

  static Sampler dropSampler(Sampler fallback, String... expressions) {
    CelBasedSamplerBuilder builder = CelBasedSampler.builder(fallback);
    for (String expression : expressions) {
      try {
        builder.drop(expression);
      } catch (CelValidationException e) {
        fail();
      }
    }
    return builder.build();
  }

  static Sampler recordAndSampleSampler(Sampler fallback, String... expressions) {
    CelBasedSamplerBuilder builder = CelBasedSampler.builder(fallback);
    for (String expression : expressions) {
      try {
        builder.recordAndSample(expression);
      } catch (CelValidationException e) {
        fail();
      }
    }
    return builder.build();
  }

  @ParameterizedTest
  @MethodSource("validSamplerConfigurations")
  void shouldCreateValidSamplerConfigurations(
      InputStream yamlStream, CelBasedSampler expectedSampler) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(yamlStream);

    Sampler sampler = PROVIDER.create(configProperties);
    assertThat(sampler).isInstanceOf(CelBasedSampler.class);

    CelBasedSampler actualSampler = (CelBasedSampler) sampler;

    verifySamplerEqualityForValidTests(actualSampler, expectedSampler);
  }

  static Stream<Arguments> validSamplerConfigurations() {
    return Stream.of(
        Arguments.of(
            loadResource("valid/drop-client-spans.yaml"),
            dropSampler(Sampler.alwaysOn(), "spanKind == \"CLIENT\"")),
        Arguments.of(
            loadResource("valid/record-specific-path.yaml"),
            recordAndSampleSampler(Sampler.alwaysOff(), "attribute[\"url.path\"] == \"/v1/user\"")),
        Arguments.of(
            loadResource("valid/multiple-drop-expressions.yaml"),
            dropSampler(
                Sampler.alwaysOff(),
                "attribute[\"http.request.method\"] == \"GET\"",
                "attribute[\"url.path\"] == \"/foo/bar\"")));
  }

  @ParameterizedTest
  @MethodSource("inValidSamplerConfigurations")
  void shouldFailWithAnInValidSamplerConfigurations(
      InputStream yamlStream, String expectedErrorMessage) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(yamlStream);

    assertThatThrownBy(() -> PROVIDER.create(configProperties))
        .isInstanceOf(DeclarativeConfigException.class)
        .hasMessage(expectedErrorMessage);
  }

  static Stream<Arguments> inValidSamplerConfigurations() {
    return Stream.of(
        Arguments.of(
            loadResource("invalid/missing-fallback-sampler.yaml"),
            "cel_based sampler .fallback_sampler is required but is null"),
        Arguments.of(
            loadResource("invalid/invalid-fallback-sampler.yaml"),
            "cel_based sampler failed to create .fallback_sampler sampler"),
        Arguments.of(
            loadResource("invalid/missing-expressions.yaml"),
            "cel_based sampler .expressions is required"),
        Arguments.of(
            loadResource("invalid/empty-expressions.yaml"),
            "cel_based sampler .expressions is required"),
        Arguments.of(
            loadResource("invalid/missing-expression.yaml"),
            "cel_based sampler .expressions[].expression is required"),
        Arguments.of(
            loadResource("invalid/missing-action.yaml"),
            "cel_based sampler .expressions[].action is required"),
        Arguments.of(
            loadResource("invalid/invalid-action.yaml"),
            "cel_based sampler .expressions[].action must be RECORD_AND_SAMPLE or DROP"));
  }

  @ParameterizedTest
  @MethodSource("inValidExpressionSamplerConfigurations")
  void shouldFailWithAnInValidExpressionSamplerConfigurations(
      InputStream yamlStream, String expectedErrorMessage) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(yamlStream);

    assertThatThrownBy(() -> PROVIDER.create(configProperties))
        .isInstanceOf(DeclarativeConfigException.class)
        .hasMessageMatching(expectedErrorMessage);
  }

  static Stream<Arguments> inValidExpressionSamplerConfigurations() {
    return Stream.of(
        Arguments.of(
            loadResource("invalid/empty-expression-value.yaml"),
            "Failed to compile CEL expression: ''\\. CEL error: ERROR: <input>:1:1: mismatched input '<EOF>' expecting(?s).*"),
        Arguments.of(
            loadResource("invalid/invalid-cel-expression.yaml"),
            "Failed to compile CEL expression: 'invalid cel expression!'\\. CEL error: ERROR: <input>:1:9: mismatched input 'cel' expecting(?s).*"));
  }
}
