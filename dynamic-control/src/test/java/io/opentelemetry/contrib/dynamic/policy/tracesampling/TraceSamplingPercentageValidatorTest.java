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

class TraceSamplingPercentageValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE =
      TraceSamplingPercentagePolicy.POLICY_TYPE;
  private static final Set<String> MAPPED_POLICY_IDS =
      new HashSet<>(Arrays.asList(TRACE_SAMPLING_POLICY_TYPE, "other-policy", "other.key"));

  private final TraceSamplingPercentageValidator validator = new TraceSamplingPercentageValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
  }

  @Test
  void testValidate_ValidJson() {
    TelemetryPolicy policy = validateJson(jsonForPercentage(50.0));

    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingPercentagePolicy.class);
    assertThat(((TraceSamplingPercentagePolicy) policy).getIdentity())
        .isEqualTo(TraceSamplingPercentagePolicy.DEFAULT_IDENTITY);
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(50.0, within(1e-9));
    assertThat(((TraceSamplingPercentagePolicy) policy).getSamplingProbability())
        .isCloseTo(0.5, within(1e-9));
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.CUSTOM);
  }

  @Test
  void validateStoresExplicitSourceKind() {
    TelemetryPolicy policy =
        validator.validate(
            first(SourceFormat.JSONKEYVALUE.parse(jsonForPercentage(50.0), MAPPED_POLICY_IDS)),
            SourceKind.OPAMP);

    assertThat(policy).isNotNull();
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.OPAMP);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 100.0})
  void testValidate_ValidJson_BoundaryValues(double percentage) {
    TelemetryPolicy policy = validateJson(jsonForPercentage(percentage));

    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingPercentagePolicy.class);
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(percentage, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_FullPolicyStruct() {
    TelemetryPolicy policy = validateJson(fullPolicyStructForPercentage("10.0"));

    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingPercentagePolicy.class);
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(10.0, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_FullPolicyStructWithOptionalFields() {
    String json =
        "{"
            + "\"id\":\"trace-sampling\","
            + "\"name\":\"Trace sampling percentage\","
            + "\"description\":\"Set the global trace sampling rate to 10%.\","
            + "\"enabled\":true,"
            + "\"created_at_unix_nano\":\"1718890000000000000\","
            + "\"modified_at_unix_nano\":\"1718893600000000000\","
            + "\"labels\":[{\"key\":\"policy.scope\","
            + "\"value\":{\"string_value\":\"global\"}}],"
            + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true,"
            + "\"negate\":false,\"case_insensitive\":false}],"
            + "\"keep\":{\"percentage\":10.0,\"mode\":\"proportional\","
            + "\"sampling_precision\":6,\"hash_seed\":0,\"fail_closed\":false}}"
            + "}";

    TelemetryPolicy policy = validateJson(json);

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(10.0, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 100.0})
  void testValidate_ValidJson_FullPolicyStructPercentageBoundaries(double percentage) {
    TelemetryPolicy policy =
        validateJson(fullPolicyStructForPercentage(Double.toString(percentage)));

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(percentage, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_ObjectShapeWithPercentageField() {
    TelemetryPolicy policy =
        validateJson("{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"percentage\": 50.0}}");

    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingPercentagePolicy.class);
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(50.0, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_ObjectShapeWithProbabilityField() {
    assertThat(validateJson("{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": 0.5}}"))
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 100.0})
  void testValidate_ValidJson_ObjectShape_BoundaryValues(double percentage) {
    TelemetryPolicy policy =
        validateJson(
            "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"percentage\": " + percentage + "}}");

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(percentage, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_PercentageAsQuotedStringInObject() {
    TelemetryPolicy policy =
        validateJson("{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"percentage\": \"62.5\"}}");

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(62.5, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_PercentageAsQuotedStringFlat() {
    TelemetryPolicy policy = validateJson("{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"37.5\"}");

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(37.5, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_MissingPolicyType() {
    assertThat(validateJson("{\"other-policy\": 50.0}")).isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 100.1})
  void testValidate_InvalidJson_PercentageOutOfRange(double percentage) {
    assertThat(validateJson(jsonForPercentage(percentage))).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.1", "100.1"})
  void testValidate_InvalidJson_FullPolicyPercentageOutOfRange(String percentage) {
    assertThat(validateJson(fullPolicyStructForPercentage(percentage))).isNull();
  }

  @Test
  void testValidate_InvalidJson_FullPolicyMissingPercentage() {
    assertThat(
            validateJson(
                "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling percentage\","
                    + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true}],"
                    + "\"keep\":{}}}"))
        .isNull();
  }

  @Test
  void testValidate_ValidJson_DoesNotInterpretGenericMatchConcern() {
    TelemetryPolicy policy =
        validateJson(
            "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling percentage\","
                + "\"trace\":{\"match\":[{\"span_attribute\":[\"db.system\"],\"exists\":true}],"
                + "\"keep\":{\"percentage\":10.0}}}");

    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(10.0, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_ValueNotNumber() {
    assertThat(validateJson("{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"high\"}")).isNull();
  }

  @Test
  void testValidate_ValidKeyValue() {
    TelemetryPolicy policy =
        validator.validate(
            first(
                SourceFormat.KEYVALUE.parse(
                    TRACE_SAMPLING_POLICY_TYPE + "=50.0", MAPPED_POLICY_IDS)),
            SourceKind.CUSTOM);

    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingPercentagePolicy.class);
    assertThat(((TraceSamplingPercentagePolicy) policy).getPercentage())
        .isCloseTo(50.0, within(1e-9));
  }

  @Test
  void testValidate_InvalidKeyValue_WrongKey() {
    assertThat(
            validator.validate(
                first(SourceFormat.KEYVALUE.parse("other.key=50.0", MAPPED_POLICY_IDS)),
                SourceKind.CUSTOM))
        .isNull();
  }

  @Test
  void testValidate_InvalidKeyValue_NotNumber() {
    assertThat(
            validator.validate(
                first(
                    SourceFormat.KEYVALUE.parse(
                        TRACE_SAMPLING_POLICY_TYPE + "=invalid", MAPPED_POLICY_IDS)),
                SourceKind.CUSTOM))
        .isNull();
  }

  private static String jsonForPercentage(double percentage) {
    return "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": " + percentage + "}";
  }

  private TelemetryPolicy validateJson(String json) {
    return validator.validate(
        first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS)), SourceKind.CUSTOM);
  }

  private static String fullPolicyStructForPercentage(String percentage) {
    return "{"
        + "\"id\":\""
        + TRACE_SAMPLING_POLICY_TYPE
        + "\","
        + "\"name\":\"Trace sampling percentage\","
        + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true}],"
        + "\"keep\":{\"percentage\":"
        + percentage
        + "}}"
        + "}";
  }

  private static SourceWrapper first(List<SourceWrapper> parsedSources) {
    assertThat(parsedSources).isNotNull();
    assertThat(parsedSources).isNotEmpty();
    return parsedSources.get(0);
  }
}
