/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.List;

/**
 * Applies validated telemetry policies to runtime components.
 *
 * <p>Implementers are notified when policies change and are responsible for translating those
 * policies into concrete runtime behavior (for example, updating a sampler). Implementers also
 * declare the {@link PolicyValidator}s they support so that only relevant policies are delivered to
 * them.
 */
public interface PolicyImplementer {

  /**
   * Called when the relevant policies have changed.
   *
   * <p>Implementers should treat the provided list as authoritative for their policy types and
   * update runtime state accordingly.
   *
   * <p>The upstream policy pipeline is assumed to have already merged policies for prioritization
   * and conflict resolution. The provided list is expected to be consistent and not contain
   * conflicting policies for the same type.
   *
   * @param policies the set of policies that apply to this implementer
   */
  void onPoliciesChanged(List<TelemetryPolicy> policies);

  /**
   * Returns the validators that this implementer supports.
   *
   * <p>These validators define which policy types and aliases the implementer can accept and
   * process.
   *
   * @return the list of supported validators
   */
  List<PolicyValidator> getValidators();
}
