/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.sampler.DelegatingSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.List;

/**
 * Implements the {@code trace-sampling} policy by updating a {@link DelegatingSampler}.
 *
 * <p>This implementer listens for validated {@link TelemetryPolicy} updates of type {@code
 * "trace-sampling"} and applies the {@code probability} field to the delegate sampler using {@link
 * Sampler#traceIdRatioBased(double)} wrapped by {@link Sampler#parentBased(Sampler)}.
 *
 * <p>If the policy spec is {@code null} (policy removal), the delegate falls back to {@link
 * Sampler#alwaysOn()}.
 *
 * <p>Validation is performed by {@link TraceSamplingValidator}; this implementer only consumes
 * policies produced by that validator.
 */
public final class TraceSamplingRatePolicyImplementer implements PolicyImplementer {

  private static final String TRACE_SAMPLING_TYPE = "trace-sampling";
  private static final String PROBABILITY_FIELD = "probability";

  private final DelegatingSampler delegatingSampler;

  /**
   * Creates a new implementer that updates the provided {@link DelegatingSampler}.
   *
   * @param delegatingSampler the sampler to update when policies change
   */
  public TraceSamplingRatePolicyImplementer(DelegatingSampler delegatingSampler) {
    this.delegatingSampler = delegatingSampler;
  }

  @Override
  public List<PolicyValidator> getValidators() {
    return Collections.singletonList(new TraceSamplingValidator());
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!TRACE_SAMPLING_TYPE.equals(policy.getType())) {
        continue;
      }
      JsonNode spec = policy.getSpec();
      if (spec == null) {
        delegatingSampler.setDelegate(Sampler.alwaysOn());
        continue;
      }
      if (spec.has(PROBABILITY_FIELD)) {
        double ratio = spec.get(PROBABILITY_FIELD).asDouble(1.0);
        Sampler sampler = Sampler.parentBased(Sampler.traceIdRatioBased(ratio));
        delegatingSampler.setDelegate(sampler);
      }
    }
  }
}
