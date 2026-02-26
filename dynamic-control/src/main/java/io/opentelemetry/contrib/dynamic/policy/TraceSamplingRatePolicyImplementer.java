/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.sampler.DelegatingSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@code trace-sampling} policy by updating a {@link DelegatingSampler}.
 *
 * <p>This implementer listens for validated {@link TelemetryPolicy} updates of type {@code
 * "trace-sampling"} and applies {@link TraceSamplingRatePolicy#getProbability()} to the delegate
 * sampler using {@link Sampler#traceIdRatioBased(double)} wrapped by {@link
 * Sampler#parentBased(Sampler)}.
 *
 * <p>If a type-only {@link TelemetryPolicy} of type {@code "trace-sampling"} is received, it is
 * treated as policy removal and the delegate falls back to {@link Sampler#alwaysOn()}.
 *
 * <p>Validation is performed by {@link TraceSamplingValidator}; this implementer only consumes
 * policies produced by that validator.
 *
 * <p>This class is thread-safe. Calls to {@link #onPoliciesChanged(List)} can occur concurrently
 * with sampling operations on the associated {@link DelegatingSampler}.
 */
public final class TraceSamplingRatePolicyImplementer implements PolicyImplementer {

  private static final List<PolicyValidator> VALIDATORS =
      Collections.<PolicyValidator>singletonList(new TraceSamplingValidator());

  private final DelegatingSampler delegatingSampler;

  /**
   * Creates a new implementer that updates the provided {@link DelegatingSampler}.
   *
   * @param delegatingSampler the sampler to update when policies change
   */
  public TraceSamplingRatePolicyImplementer(DelegatingSampler delegatingSampler) {
    Objects.requireNonNull(delegatingSampler, "delegatingSampler cannot be null");
    this.delegatingSampler = delegatingSampler;
  }

  @Override
  public List<PolicyValidator> getValidators() {
    return VALIDATORS;
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!TraceSamplingRatePolicy.TYPE.equals(policy.getType())) {
        continue;
      }
      if (!(policy instanceof TraceSamplingRatePolicy)) {
        // Type-only policy represents removing trace-sampling config.
        delegatingSampler.setDelegate(Sampler.alwaysOn());
        continue;
      }
      double ratio = ((TraceSamplingRatePolicy) policy).getProbability();
      Sampler sampler = Sampler.parentBased(Sampler.traceIdRatioBased(ratio));
      delegatingSampler.setDelegate(sampler);
    }
  }
}
