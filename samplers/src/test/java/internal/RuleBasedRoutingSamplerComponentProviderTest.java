/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.internal.RuleBasedRoutingSamplerComponentProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.StructuredConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.FileConfiguration;
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

class RuleBasedRoutingSamplerComponentProviderTest {

  private static final RuleBasedRoutingSamplerComponentProvider PROVIDER =
      new RuleBasedRoutingSamplerComponentProvider();

  @Test
  void endToEnd() {
    String yaml =
        "file_format: 0.1\n"
            + "tracer_provider:\n"
            + "  sampler:\n"
            + "    parent_based:\n"
            + "      root:\n"
            + "        rule_based_routing:\n"
            + "          fallback_sampler:\n"
            + "            always_on:\n"
            + "          span_kind: SERVER\n"
            + "          rules:\n"
            + "            - attribute: url.path\n"
            + "              pattern: /actuator.*\n"
            + "              action: DROP\n";
    OpenTelemetrySdk openTelemetrySdk =
        FileConfiguration.parseAndCreate(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Sampler sampler = openTelemetrySdk.getSdkTracerProvider().getSampler();
    assertThat(sampler.toString())
        .isEqualTo(
            "ParentBased{"
                + "root:RuleBasedRoutingSampler{"
                + "rules=["
                + "SamplingRule{attributeKey=url.path, delegate=AlwaysOffSampler, pattern=/actuator.*}"
                + "], "
                + "kind=SERVER, "
                + "fallback=AlwaysOnSampler"
                + "},"
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
                "GET /actuator/health",
                SpanKind.SERVER,
                Attributes.builder().put("url.path", "/v1/users").build(),
                Collections.emptyList()))
        .isEqualTo(SamplingResult.recordAndSample());
  }

  @ParameterizedTest
  @MethodSource("createValidArgs")
  void create_Valid(String yaml, RuleBasedRoutingSampler expectedSampler) {
    StructuredConfigProperties configProperties =
        FileConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    Sampler sampler = PROVIDER.create(configProperties);
    assertThat(sampler.toString()).isEqualTo(expectedSampler.toString());
  }

  static Stream<Arguments> createValidArgs() {
    return Stream.of(
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "span_kind: SERVER\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n"
                + "    action: DROP\n",
            RuleBasedRoutingSampler.builder(SpanKind.SERVER, Sampler.alwaysOn())
                .drop(AttributeKey.stringKey("url.path"), "path")
                .build()),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_off:\n"
                + "span_kind: SERVER\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n"
                + "    action: RECORD_AND_SAMPLE\n",
            RuleBasedRoutingSampler.builder(SpanKind.SERVER, Sampler.alwaysOff())
                .recordAndSample(AttributeKey.stringKey("url.path"), "path")
                .build()),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_off:\n"
                + "span_kind: CLIENT\n"
                + "rules:\n"
                + "  - attribute: http.request.method\n"
                + "    pattern: GET\n"
                + "    action: DROP\n"
                + "  - attribute: url.path\n"
                + "    pattern: /foo/bar\n"
                + "    action: DROP\n",
            RuleBasedRoutingSampler.builder(SpanKind.CLIENT, Sampler.alwaysOff())
                .drop(AttributeKey.stringKey("http.request.method"), "GET")
                .drop(AttributeKey.stringKey("url.path"), "/foo/bar")
                .build()));
  }

  @ParameterizedTest
  @MethodSource("createInvalidArgs")
  void create_Invalid(String yaml, String expectedErrorMessage) {
    StructuredConfigProperties configProperties =
        FileConfiguration.toConfigProperties(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThatThrownBy(() -> PROVIDER.create(configProperties))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(expectedErrorMessage);
  }

  static Stream<Arguments> createInvalidArgs() {
    return Stream.of(
        Arguments.of(
            "span_kind: SERVER\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n",
            "rule_based_routing sampler .fallback is required but is null"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  foo:\n"
                + "span_kind: foo\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n",
            "rule_Based_routing sampler failed to create .fallback sampler"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "span_kind: foo\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n",
            "rule_based_routing sampler .span_kind is invalid: foo"),
        Arguments.of(
            "fallback_sampler:\n" + "  always_on:\n" + "span_kind: SERVER\n",
            "rule_based_routing sampler .rules is required"),
        Arguments.of(
            "fallback_sampler:\n" + "  always_on:\n" + "span_kind: SERVER\n" + "rules: []\n",
            "rule_based_routing sampler .rules is required"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "span_kind: SERVER\n"
                + "rules:\n"
                + "  - attribute: url.path\n",
            "rule_based_routing sampler .rules[].pattern is required"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "span_kind: SERVER\n"
                + "rules:\n"
                + "  - pattern: path\n",
            "rule_based_routing sampler .rules[].attribute is required"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "span_kind: SERVER\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n",
            "rule_based_routing sampler .rules[].action is required"),
        Arguments.of(
            "fallback_sampler:\n"
                + "  always_on:\n"
                + "span_kind: SERVER\n"
                + "rules:\n"
                + "  - attribute: url.path\n"
                + "    pattern: path\n"
                + "    action: foo\n",
            "rule_based_routing sampler .rules[].action is must be RECORD_AND_SAMPLE or DROP"));
  }
}
