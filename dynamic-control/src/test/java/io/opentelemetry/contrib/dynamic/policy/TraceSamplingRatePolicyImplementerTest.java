/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.dynamic.sampler.DelegatingSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceSamplingRatePolicyImplementerTest {

  @Test
  void nullSpecFallsBackToAlwaysOn() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(singletonList(new TelemetryPolicy("trace-sampling")));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void appliesProbabilityToDelegate() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(singletonList(new TraceSamplingRatePolicy(1.0)));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void ignoresUnrelatedPolicyTypes() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(singletonList(new TelemetryPolicy("other-policy")));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.DROP);
  }

  @Test
  void typeOnlyTraceSamplingPolicyFallsBackToAlwaysOn() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(singletonList(new TelemetryPolicy("trace-sampling")));
    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
  }

  @Test
  void lastTraceSamplingPolicyWins() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    List<TelemetryPolicy> policies =
        Arrays.asList(new TraceSamplingRatePolicy(0.0), new TraceSamplingRatePolicy(1.0));

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
}
