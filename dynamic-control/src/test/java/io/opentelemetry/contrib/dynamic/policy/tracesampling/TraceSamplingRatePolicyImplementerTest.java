/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.dynamic.policy.DeletedTelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicyIdentity;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceSamplingRatePolicyImplementerTest {

  @Test
  void deletedTraceSamplingPolicyFallsBackToAlwaysOn() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(
        singletonList(
            new DeletedTelemetryPolicy(
                TraceSamplingRatePolicy.DEFAULT_IDENTITY, TraceSamplingRatePolicy.POLICY_TYPE)));

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
  void skipsRepeatedEquivalentProbability() {
    CountingDelegatingSampler delegatingSampler = new CountingDelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(singletonList(new TraceSamplingRatePolicy(1.0)));
    implementer.onPoliciesChanged(singletonList(new TraceSamplingRatePolicy(1.0)));

    assertThat(delegatingSampler.changedProbabilityCount).isEqualTo(1);
  }

  @Test
  void ignoresUnrelatedPolicyTypes() {
    DelegatingSampler delegatingSampler = new DelegatingSampler(Sampler.alwaysOff());
    TraceSamplingRatePolicyImplementer implementer =
        new TraceSamplingRatePolicyImplementer(delegatingSampler);

    implementer.onPoliciesChanged(singletonList(new TestTelemetryPolicy("other-policy")));

    assertThat(decisionFor(delegatingSampler)).isEqualTo(SamplingDecision.DROP);
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

  private static final class CountingDelegatingSampler extends DelegatingSampler {
    private int changedProbabilityCount;

    private CountingDelegatingSampler(Sampler initialDelegate) {
      super(initialDelegate);
    }

    @Override
    public synchronized boolean setSamplingProbability(double probability) {
      boolean changed = super.setSamplingProbability(probability);
      if (changed) {
        changedProbabilityCount++;
      }
      return changed;
    }
  }

  private static final class TestTelemetryPolicy implements TelemetryPolicy {
    private final TelemetryPolicyIdentity identity;
    private final String type;

    private TestTelemetryPolicy(String type) {
      this.identity = new TelemetryPolicyIdentity(type, "Test policy");
      this.type = type;
    }

    @Override
    public TelemetryPolicyIdentity getIdentity() {
      return identity;
    }

    @Override
    public String getType() {
      return type;
    }
  }
}
