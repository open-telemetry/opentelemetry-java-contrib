/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingRateValidatorTest {
  private static final Set<String> MAPPED_POLICY_IDS =
      Collections.singleton(TraceSamplingRatePolicy.POLICY_TYPE);
  private final TraceSamplingRateValidator validator = new TraceSamplingRateValidator();

  @Test
  void getPolicyTypeReturnsTraceSampling() {
    assertThat(validator.getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void validatesFlatJsonRatio() {
    TraceSamplingRatePolicy policy = validateJson("{\"trace-sampling\": 0.25}", SourceKind.CUSTOM);

    assertThat(policy).isNotNull();
    assertThat(policy.getSamplingProbability()).isEqualTo(0.25);
    assertThat(policy.getType()).isEqualTo("trace-sampling");
  }

  @Test
  void validatesJsonRatioKeyword() {
    TraceSamplingRatePolicy policy =
        validateJson("{\"trace-sampling\": {\"ratio\": 0.75}}", SourceKind.OPAMP);

    assertThat(policy).isNotNull();
    assertThat(policy.getSamplingProbability()).isEqualTo(0.75);
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.OPAMP);
  }

  @Test
  void rejectsProbabilityKeyword() {
    assertThat(validateJson("{\"trace-sampling\": {\"probability\": 0.5}}", SourceKind.CUSTOM))
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void rejectsOutOfRangeRatios(double ratio) {
    assertThat(validateJson("{\"trace-sampling\": " + ratio + "}", SourceKind.CUSTOM)).isNull();
  }

  @Test
  void validatesKeyValueRatio() {
    SourceWrapper source =
        first(SourceFormat.KEYVALUE.parse("trace-sampling=0.5", MAPPED_POLICY_IDS));
    TelemetryPolicy policy = validator.validate(source, SourceKind.CUSTOM);

    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getSamplingProbability()).isEqualTo(0.5);
  }

  private TraceSamplingRatePolicy validateJson(String json, SourceKind sourceKind) {
    SourceWrapper source = first(SourceFormat.JSONKEYVALUE.parse(json, MAPPED_POLICY_IDS));
    TelemetryPolicy policy = validator.validate(source, sourceKind);
    return policy instanceof TraceSamplingRatePolicy ? (TraceSamplingRatePolicy) policy : null;
  }

  private static SourceWrapper first(List<SourceWrapper> parsedSources) {
    assertThat(parsedSources).isNotNull();
    assertThat(parsedSources).isNotEmpty();
    return parsedSources.get(0);
  }
}
