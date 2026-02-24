/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE = "trace-sampling";
  private static final String TRACE_SAMPLING_ALIAS = "trace-sampling.probability";

  private final TraceSamplingValidator validator = new TraceSamplingValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
  }

  @Test
  void testGetAlias() {
    assertThat(validator.getAlias()).isEqualTo(TRACE_SAMPLING_ALIAS);
  }

  @Test
  void testValidate_ValidJson() {
    String json = jsonForProbability(0.5);
    TelemetryPolicy policy = validator.validate(json);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_BoundaryValues(double probability) {
    String json = jsonForProbability(probability);
    TelemetryPolicy policy = validator.validate(json);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(probability, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_Malformed() {
    String json = "{invalid-json";
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidate_InvalidJson_MissingPolicyType() {
    String json = "{\"other-policy\": {\"probability\": 0.5}}";
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidate_InvalidJson_MissingProbability() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"other-field\": 0.5}}";
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidate_InvalidJson_ProbabilityNotNumber() {
    String json =
        "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": \"high\"}}";
    assertThat(validator.validate(json)).isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void testValidate_InvalidJson_ProbabilityOutOfRange(double probability) {
    String json = jsonForProbability(probability);
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidateAlias_Valid() {
    TelemetryPolicy policy = validator.validateAlias(TRACE_SAMPLING_ALIAS, "0.5");
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.0", "1.0"})
  void testValidateAlias_Valid_BoundaryValues(String probability) {
    TelemetryPolicy policy = validator.validateAlias(TRACE_SAMPLING_ALIAS, probability);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(Double.parseDouble(probability), within(1e-9));
  }

  @Test
  void testValidateAlias_InvalidKey() {
    assertThat(validator.validateAlias("other.key", "0.5")).isNull();
  }

  @Test
  void testValidateAlias_InvalidValue_NotNumber() {
    assertThat(validator.validateAlias(TRACE_SAMPLING_ALIAS, "invalid")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.1", "1.1"})
  void testValidateAlias_InvalidValue_OutOfRange(String probability) {
    assertThat(validator.validateAlias(TRACE_SAMPLING_ALIAS, probability)).isNull();
  }

  private static String jsonForProbability(double probability) {
    return "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": " + probability + "}}";
  }
}
