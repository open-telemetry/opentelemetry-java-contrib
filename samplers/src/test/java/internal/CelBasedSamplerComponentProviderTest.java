/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package internal;

import static io.opentelemetry.sdk.trace.samplers.SamplingResult.recordAndSample;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.cel.common.CelValidationException;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.sampler.CelBasedSampler;
import io.opentelemetry.contrib.sampler.CelBasedSamplerBuilder;
import io.opentelemetry.contrib.sampler.internal.CelBasedSamplerComponentProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CelBasedSamplerComponentProviderTest {

  private static final CelBasedSamplerComponentProvider PROVIDER =
      new CelBasedSamplerComponentProvider();

  @Test
  void endToEnd() {
    String yaml =
        "file_format: 0.4\n"
            + "tracer_provider:\n"
            + "  sampler:\n"
            + "    parent_based:\n"
            + "      root:\n"
            + "        cel_based:\n"
            + "          fallback_sampler:\n"
            + "            always_on:\n"
            + "          expressions:\n"
            + "            - action: DROP\n"
            + "              expression: 'spanKind == \"SERVER\" && attribute[\"url.path\"].matches(\"/actuator.*\")'\n";
    OpenTelemetrySdk openTelemetrySdk =
        DeclarativeConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Sampler sampler = openTelemetrySdk.getSdkTracerProvider().getSampler();
    assertThat(sampler.toString())
        .isEqualTo(
            "ParentBased{"
                + "root:CelBasedSampler{"
                + "fallback=AlwaysOnSampler, "
                + "expressions=["
                + "CelBasedSamplingExpression{"
                + "delegate=AlwaysOffSampler, "
                + "expression='spanKind == \"SERVER\" && attribute[\"url.path\"].matches(\"/actuator.*\")"
                + "}"
                + "]},"
                + "remoteParentSampled:AlwaysOnSampler,"
                + "remoteParentNotSampled:AlwaysOffSampler,"
                + "localParentSampled:AlwaysOnSampler,"
                + "localParentNotSampled:AlwaysOffSampler"
                + "}");

    // SERVER span to /actuator.* path should be dropped
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
        // Delegate to the provider to handle the exception
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
        // Delegate to the provider to handle the exception
      }
    }
    return builder.build();
  }

  @ParameterizedTest
  @MethodSource("createValidArgs")
  void create_Valid(String yaml, CelBasedSampler expectedSampler) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    Sampler sampler = PROVIDER.create(configProperties);
    assertThat(sampler.toString()).isEqualTo(expectedSampler.toString());
  }

  static Stream<Arguments> createValidArgs() {
    return Stream.of(
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "expressions:\n"
                + "  - action: DROP\n"
                + "    expression: 'spanKind == \"CLIENT\"'\n",
            dropSampler(Sampler.alwaysOn(), "spanKind == \"CLIENT\"")),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_off:\n"
                + "expressions:\n"
                + "  - action: RECORD_AND_SAMPLE\n"
                + "    expression: 'attribute[\"url.path\"] == \"/v1/user\"'\n",
            recordAndSampleSampler(Sampler.alwaysOff(), "attribute[\"url.path\"] == \"/v1/user\"")),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_off:\n"
                + "expressions:\n"
                + "  - action: DROP\n"
                + "    expression: 'attribute[\"http.request.method\"] == \"GET\"'\n"
                + "  - action: DROP\n"
                + "    expression: 'attribute[\"url.path\"] == \"/foo/bar\"'\n"
                + "    action: DROP\n",
            dropSampler(
                Sampler.alwaysOff(),
                "attribute[\"http.request.method\"] == \"GET\"",
                "attribute[\"url.path\"] == \"/foo/bar\"")));
  }

  @ParameterizedTest
  @MethodSource("createInvalidArgs")
  void create_Invalid(String yaml, String expectedErrorMessage) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThatThrownBy(() -> PROVIDER.create(configProperties))
        .isInstanceOf(DeclarativeConfigException.class)
        .hasMessage(expectedErrorMessage);
  }

  static Stream<Arguments> createInvalidArgs() {
    return Stream.of(
        Arguments.of(
            "expressions:\n" + "  - action: DROP\n" + "    expression: 'spanKind == \"CLIENT\"'\n",
            "cel_based sampler .fallback_sampler is required but is null"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  foo:\n"
                + "expressions:\n"
                + "  - action: DROP\n"
                + "    expression: 'spanKind == \"CLIENT\"'\n",
            "cel_based sampler failed to create .fallback_sampler sampler"),
        Arguments.of(
            "fallback_sampler:\n" + "  always_on: {}\n",
            "cel_based sampler .expressions is required"),
        Arguments.of(
            "fallback_sampler:\n" + "  always_on: {}\n" + "expressions: []\n",
            "cel_based sampler .expressions is required"),
        Arguments.of(
            "fallback_sampler:\n" + "  always_on: {}\n" + "expressions:\n" + "  - action: DROP\n",
            "cel_based sampler .expressions[].expression is required"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on: {}\n"
                + "expressions:\n"
                + "  - expression: 'spanKind == \"CLIENT\"'\n",
            "cel_based sampler .expressions[].action is required"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on: {}\n"
                + "expressions:\n"
                + "  - action: INVALID\n"
                + "    expression: 'spanKind == \"CLIENT\"'\n",
            "cel_based sampler .expressions[].action must be RECORD_AND_SAMPLE or DROP"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on: {}\n"
                + "expressions:\n"
                + "  - action: DROP\n"
                + "    expression: 'invalid cel expression!'\n",
            "Failed to compile CEL expression: invalid cel expression!"));
  }
}
