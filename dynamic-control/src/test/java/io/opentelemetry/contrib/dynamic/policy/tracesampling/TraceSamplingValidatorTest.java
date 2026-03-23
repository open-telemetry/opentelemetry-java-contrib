/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE = TraceSamplingRatePolicy.TYPE;

  private final TraceSamplingValidator validator = new TraceSamplingValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
  }

  @Test
  void testValidate_ValidJson() {
    String json = jsonForProbability(0.5);
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_BoundaryValues(double probability) {
    String json = jsonForProbability(probability);
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(probability, within(1e-9));
  }

  /**
   * Regression: {@link TraceSamplingValidator#validateJsonValue} still accepts the legacy nested
   * object shape {@code {"trace-sampling": {"probability": <n>}}} used before flat numeric values
   * were introduced.
   */
  @Test
  void testValidate_ValidJson_LegacyObjectShapeWithProbabilityField() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": 0.5}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_LegacyObjectShape_BoundaryValues(double probability) {
    String json =
        "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": " + probability + "}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(probability, within(1e-9));
  }

  /**
   * String probabilities in JSON (object or flat) are accepted via {@code parseDouble} on textual
   * nodes — keeps migration-compatible configs that quote numeric values.
   */
  @Test
  void testValidate_ValidJson_ProbabilityAsQuotedStringInLegacyObject() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": \"0.625\"}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.625, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_ProbabilityAsQuotedStringFlat() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"0.375\"}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.375, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_MissingPolicyType() {
    String json = "{\"other-policy\": 0.5}";
    assertThat(validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)))).isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void testValidate_InvalidJson_ProbabilityOutOfRange(double probability) {
    String json = jsonForProbability(probability);
    assertThat(validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)))).isNull();
  }

  @Test
  void testValidate_InvalidJson_ValueNotNumber() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"high\"}";
    assertThat(validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)))).isNull();
  }

  @Test
  void testValidate_ValidKeyValue() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=0.5";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_InvalidKeyValue_WrongKey() {
    assertThat(validator.validate(first(SourceFormat.KEYVALUE.parse("other.key=0.5")))).isNull();
  }

  @Test
  void testValidate_InvalidKeyValue_NotNumber() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=invalid";
    assertThat(validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)))).isNull();
  }

  private static String jsonForProbability(double probability) {
    return "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": " + probability + "}";
  }

  private static SourceWrapper first(List<SourceWrapper> parsedSources) {
    assertThat(parsedSources).isNotNull();
    assertThat(parsedSources).isNotEmpty();
    return parsedSources.get(0);
  }
}
