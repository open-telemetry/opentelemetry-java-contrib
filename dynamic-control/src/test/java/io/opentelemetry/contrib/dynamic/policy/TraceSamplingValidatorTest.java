/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TraceSamplingValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE = "trace-sampling";
  private static final String PROBABILITY_FIELD = "probability";
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
    assertThat(policy.getSpec().get(PROBABILITY_FIELD).asDouble()).isEqualTo(0.5);
  }

  @Test
  void testValidate_ValidJson_BoundaryValues() {
    String json0 = jsonForProbability(0.0);
    TelemetryPolicy policy0 = validator.validate(json0);
    assertThat(policy0).isNotNull();
    assertThat(policy0.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy0.getSpec().get(PROBABILITY_FIELD).asDouble()).isEqualTo(0.0);

    String json1 = jsonForProbability(1.0);
    TelemetryPolicy policy1 = validator.validate(json1);
    assertThat(policy1).isNotNull();
    assertThat(policy1.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy1.getSpec().get(PROBABILITY_FIELD).asDouble()).isEqualTo(1.0);
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
        "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"" + PROBABILITY_FIELD + "\": \"high\"}}";
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidate_InvalidJson_ProbabilityOutOfRange() {
    String jsonLow = jsonForProbability(-0.1);
    assertThat(validator.validate(jsonLow)).isNull();

    String jsonHigh = jsonForProbability(1.1);
    assertThat(validator.validate(jsonHigh)).isNull();
  }

  @Test
  void testValidateAlias_Valid() {
    TelemetryPolicy policy = validator.validateAlias(TRACE_SAMPLING_ALIAS, "0.5");
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy.getSpec().get(PROBABILITY_FIELD).asDouble()).isEqualTo(0.5);
  }

  @Test
  void testValidateAlias_Valid_BoundaryValues() {
    TelemetryPolicy policy0 = validator.validateAlias(TRACE_SAMPLING_ALIAS, "0.0");
    assertThat(policy0).isNotNull();
    assertThat(policy0.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy0.getSpec().get(PROBABILITY_FIELD).asDouble()).isEqualTo(0.0);

    TelemetryPolicy policy1 = validator.validateAlias(TRACE_SAMPLING_ALIAS, "1.0");
    assertThat(policy1).isNotNull();
    assertThat(policy1.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy1.getSpec().get(PROBABILITY_FIELD).asDouble()).isEqualTo(1.0);
  }

  @Test
  void testValidateAlias_InvalidKey() {
    assertThat(validator.validateAlias("other.key", "0.5")).isNull();
  }

  @Test
  void testValidateAlias_InvalidValue_NotNumber() {
    assertThat(validator.validateAlias(TRACE_SAMPLING_ALIAS, "invalid")).isNull();
  }

  @Test
  void testValidateAlias_InvalidValue_OutOfRange() {
    assertThat(validator.validateAlias(TRACE_SAMPLING_ALIAS, "-0.1")).isNull();
    assertThat(validator.validateAlias(TRACE_SAMPLING_ALIAS, "1.1")).isNull();
  }

  private static String jsonForProbability(double probability) {
    return "{\""
        + TRACE_SAMPLING_POLICY_TYPE
        + "\": {\""
        + PROBABILITY_FIELD
        + "\": "
        + probability
        + "}}";
  }
}
