/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.dynamic.sampler.DelegatingSampler;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceSamplingRatePolicyImplementerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void nullSpecFallsBackToAlwaysOn() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(
        Collections.singletonList(new TelemetryPolicy("trace-sampling", null)));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void appliesProbabilityToDelegate() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(
        Collections.singletonList(new TelemetryPolicy("trace-sampling", spec("probability", 1.0))));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void ignoresUnrelatedPolicyTypes() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(
        Collections.singletonList(new TelemetryPolicy("other-policy", spec("value", 1.0))));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void lastTraceSamplingPolicyWins() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    List<TelemetryPolicy> policies =
        Arrays.asList(
            new TelemetryPolicy("trace-sampling", spec("probability", 0.0)),
            new TelemetryPolicy("trace-sampling", spec("probability", 1.0)));

    implementer.onPoliciesChanged(policies);

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  private static SamplingDecision decisionFor(DelegatingSampler sampler) {
    SamplingResult result =
        sampler.shouldSample(
            Context.root(),
            "00000000000000000000000000000001",
            "test-span",
            SpanKind.INTERNAL,
            Attributes.empty(),
            Collections.emptyList());
    return result.getDecision();
  }

  private static JsonNode spec(String field, double value) {
    return MAPPER.createObjectNode().put(field, value);
  }
}
