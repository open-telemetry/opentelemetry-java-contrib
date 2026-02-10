/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TraceSamplingValidatorTest {

  private final TraceSamplingValidator validator = new TraceSamplingValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void testGetAlias() {
    assertThat(validator.getAlias()).isEqualTo("trace-sampling.probability");
  }

  @Test
  void testValidate_ValidJson() {
    String json = "{\"trace-sampling\": {\"probability\": 0.5}}";
    TelemetryPolicy policy = validator.validate(json);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo("trace-sampling");
    assertThat(policy.getSpec().get("probability").asDouble()).isEqualTo(0.5);
  }

  @Test
  void testValidate_ValidJson_BoundaryValues() {
    String json0 = "{\"trace-sampling\": {\"probability\": 0.0}}";
    assertThat(validator.validate(json0)).isNotNull();

    String json1 = "{\"trace-sampling\": {\"probability\": 1.0}}";
    assertThat(validator.validate(json1)).isNotNull();
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
    String json = "{\"trace-sampling\": {\"other-field\": 0.5}}";
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidate_InvalidJson_ProbabilityNotNumber() {
    String json = "{\"trace-sampling\": {\"probability\": \"high\"}}";
    assertThat(validator.validate(json)).isNull();
  }

  @Test
  void testValidate_InvalidJson_ProbabilityOutOfRange() {
    String jsonLow = "{\"trace-sampling\": {\"probability\": -0.1}}";
    assertThat(validator.validate(jsonLow)).isNull();

    String jsonHigh = "{\"trace-sampling\": {\"probability\": 1.1}}";
    assertThat(validator.validate(jsonHigh)).isNull();
  }

  @Test
  void testValidateAlias_Valid() {
    TelemetryPolicy policy = validator.validateAlias("trace-sampling.probability", "0.5");
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo("trace-sampling");
    assertThat(policy.getSpec().get("probability").asDouble()).isEqualTo(0.5);
  }

  @Test
  void testValidateAlias_Valid_BoundaryValues() {
    assertThat(validator.validateAlias("trace-sampling.probability", "0.0")).isNotNull();
    assertThat(validator.validateAlias("trace-sampling.probability", "1.0")).isNotNull();
  }

  @Test
  void testValidateAlias_InvalidKey() {
    assertThat(validator.validateAlias("other.key", "0.5")).isNull();
  }

  @Test
  void testValidateAlias_InvalidValue_NotNumber() {
    assertThat(validator.validateAlias("trace-sampling.probability", "invalid")).isNull();
  }

  @Test
  void testValidateAlias_InvalidValue_OutOfRange() {
    assertThat(validator.validateAlias("trace-sampling.probability", "-0.1")).isNull();
    assertThat(validator.validateAlias("trace-sampling.probability", "1.1")).isNull();
  }
}
