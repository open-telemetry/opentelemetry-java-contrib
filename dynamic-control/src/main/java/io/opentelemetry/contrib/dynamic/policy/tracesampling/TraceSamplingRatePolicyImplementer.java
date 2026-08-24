/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Implements trace sampling policies by updating a {@link DelegatingSampler}.
 *
 * <p>Policy representations are converted to a normalized sampling probability before being
 * applied.
 *
 * <p>This class is thread-safe. Calls to {@link #onPoliciesChanged(List)} can occur concurrently
 * with sampling operations on the associated {@link DelegatingSampler}.
 */
public final class TraceSamplingRatePolicyImplementer implements PolicyImplementer {
  private static final Logger logger =
      Logger.getLogger(TraceSamplingRatePolicyImplementer.class.getName());

  private static final List<PolicyValidator> VALIDATORS =
      Collections.<PolicyValidator>singletonList(new TraceSamplingValidator());

  private final DelegatingSampler delegatingSampler;
  private final List<PolicyValidator> validators;

  /**
   * Creates a new implementer that updates the provided {@link DelegatingSampler}.
   *
   * @param delegatingSampler the sampler to update when policies change
   */
  public TraceSamplingRatePolicyImplementer(DelegatingSampler delegatingSampler) {
    this(delegatingSampler, VALIDATORS);
  }

  TraceSamplingRatePolicyImplementer(
      DelegatingSampler delegatingSampler, List<PolicyValidator> validators) {
    this.delegatingSampler =
        Objects.requireNonNull(delegatingSampler, "delegatingSampler cannot be null");
    this.validators =
        Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(validators, "validators cannot be null")));
  }

  @Override
  public List<PolicyValidator> getValidators() {
    return validators;
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!supports(policy.getType())) {
        continue;
      }
      if (policy.isDeleted()) {
        applySamplingProbability(1.0, "reset");
        continue;
      }
      if (!(policy instanceof AbstractTraceSamplingPolicy)) {
        continue;
      }
      double probability = ((AbstractTraceSamplingPolicy) policy).getSamplingProbability();
      applySamplingProbability(probability, "update");
    }
  }

  private boolean supports(String policyType) {
    for (PolicyValidator validator : validators) {
      if (validator.getPolicyType().equals(policyType)) {
        return true;
      }
    }
    return false;
  }

  private void applySamplingProbability(double probability, String action) {
    if (delegatingSampler.setSamplingProbability(probability)) {
      logger.info("Applied trace sampling policy " + action + ": probability=" + probability);
    }
  }
}
