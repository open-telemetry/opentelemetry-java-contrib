/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE = TraceSamplingRatePolicy.POLICY_TYPE;
  private static final Set<String> MAPPED_POLICY_IDS =
      new HashSet<>(Arrays.asList(TRACE_SAMPLING_POLICY_TYPE, "other-policy", "other.key"));

  private final TraceSamplingValidator validator = new TraceSamplingValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
  }

  @Test
  void testValidate_ValidJson() {
    String json = jsonForProbability(0.5);
    TelemetryPolicy policy =
        validator.validate(
            first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getIdentity())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.CUSTOM);
  }

  @Test
  void validateStoresExplicitSourceKind() {
    String json = jsonForProbability(0.5);
    TelemetryPolicy policy =
        validator.validate(
            first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.OPAMP);

    assertThat(policy).isNotNull();
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.OPAMP);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_BoundaryValues(double probability) {
    String json = jsonForProbability(probability);
    TelemetryPolicy policy =
        validator.validate(
            first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(probability, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_FullPolicyStruct() {
    TelemetryPolicy policy = validateJson(fullPolicyStructForRatio("0.1"));

    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.1, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_FullPolicyStructWithOptionalFields() {
    String json =
        "{"
            + "\"id\":\"trace-sampling\","
            + "\"name\":\"Trace sampling rate\","
            + "\"description\":\"Set the global trace sampling rate to 10%.\","
            + "\"enabled\":true,"
            + "\"created_at_unix_nano\":\"1718890000000000000\","
            + "\"modified_at_unix_nano\":\"1718893600000000000\","
            + "\"labels\":[{\"key\":\"policy.scope\","
            + "\"value\":{\"string_value\":\"global\"}}],"
            + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true,"
            + "\"negate\":false,\"case_insensitive\":false}],"
            + "\"keep\":{\"probability\":0.1,\"mode\":\"proportional\","
            + "\"sampling_precision\":6,\"hash_seed\":0,\"fail_closed\":false}}"
            + "}";

    TelemetryPolicy policy = validateJson(json);

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.1, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_FullPolicyStructRatioBoundaries(double ratio) {
    TelemetryPolicy policy = validateJson(fullPolicyStructForRatio(Double.toString(ratio)));

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(ratio, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_RatioAsQuotedStringFlat() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"0.375\"}";
    TelemetryPolicy policy =
        validator.validate(
            first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM);
    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.375, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_MissingPolicyType() {
    String json = "{\"other-policy\": 0.5}";
    assertThat(
            validator.validate(
                first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM))
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void testValidate_InvalidJson_ProbabilityOutOfRange(double probability) {
    String json = jsonForProbability(probability);
    assertThat(
            validator.validate(
                first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM))
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.1", "1.1"})
  void testValidate_InvalidJson_FullPolicyRatioOutOfRange(String ratio) {
    assertThat(validateJson(fullPolicyStructForRatio(ratio))).isNull();
  }

  @Test
  void testValidate_InvalidJson_FullPolicyMissingRatio() {
    assertThat(
            validateJson(
                "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling rate\","
                    + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true}],"
                    + "\"keep\":{}}}"))
        .isNull();
  }

  @Test
  void testValidate_ValidJson_DoesNotInterpretGenericMatchConcern() {
    TelemetryPolicy policy =
        validateJson(
            "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling rate\","
                + "\"trace\":{\"match\":[{\"span_attribute\":[\"db.system\"],\"exists\":true}],"
                + "\"keep\":{\"probability\":0.1}}}");

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.1, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_ValueNotNumber() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"high\"}";
    assertThat(
            validator.validate(
                first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM))
        .isNull();
  }

  @Test
  void testValidate_ValidKeyValue() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=0.5";
    TelemetryPolicy policy =
        validator.validate(
            first(SourceFormat.KEYVALUE.parse(keyValue, MAPPED_POLICY_IDS)), SourceKind.CUSTOM);
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getIdentity())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_InvalidKeyValue_WrongKey() {
    assertThat(
            validator.validate(
                first(SourceFormat.KEYVALUE.parse("other.key=0.5", MAPPED_POLICY_IDS)),
                SourceKind.CUSTOM))
        .isNull();
  }

  @Test
  void testValidate_InvalidKeyValue_NotNumber() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=invalid";
    assertThat(
            validator.validate(
                first(SourceFormat.KEYVALUE.parse(keyValue, MAPPED_POLICY_IDS)), SourceKind.CUSTOM))
        .isNull();
  }

  private static String jsonForProbability(double probability) {
    return "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": " + probability + "}";
  }

  private TelemetryPolicy validateJson(String json) {
    return validator.validate(
        first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM);
  }

  private static String fullPolicyStructForRatio(String ratio) {
    return "{"
        + "\"id\":\""
        + TRACE_SAMPLING_POLICY_TYPE
        + "\","
        + "\"name\":\"Trace sampling rate\","
        + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true}],"
        + "\"keep\":{\"probability\":"
        + ratio
        + "}}"
        + "}";
  }

  private static SourceWrapper first(List<SourceWrapper> parsedSources) {
    assertThat(parsedSources).isNotNull();
    assertThat(parsedSources).isNotEmpty();
    return parsedSources.get(0);
  }
}
