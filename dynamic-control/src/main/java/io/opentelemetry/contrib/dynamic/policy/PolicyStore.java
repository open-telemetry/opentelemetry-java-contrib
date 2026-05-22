/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Holds the latest validated policy snapshot and reports whether an update changed effective
 * configuration.
 */
public final class PolicyStore {

  private List<TelemetryPolicy> policies = Collections.emptyList();
  private final List<PolicyImplementer> implementers = new ArrayList<>();

  /**
   * Replaces the stored policies when the new snapshot is not equal to the current one.
   *
   * <p>Input lists are normalized to a set of distinct policies ({@link TelemetryPolicy#equals
   * value equality}): duplicates are dropped and only the first occurrence of each policy is kept
   * (insertion order). Change detection uses set equality, so list order does not matter. That
   * matches telemetry policy semantics where the effective result does not depend on processing
   * order (see the telemetry policy OTEP, commutativity / no user-defined ordering between
   * policies).
   *
   * @return {@code true} if the store was updated, {@code false} if the snapshot was unchanged
   */
  public boolean updatePolicies(List<TelemetryPolicy> newPolicies) {
    Objects.requireNonNull(newPolicies, "newPolicies cannot be null");
    LinkedHashSet<TelemetryPolicy> newPolicySet = new LinkedHashSet<>(newPolicies);
    List<TelemetryPolicy> policiesSnapshot;
    List<PolicyImplementer> implementersSnapshot;
    synchronized (this) {
      if (new LinkedHashSet<>(policies).equals(newPolicySet)) {
        return false;
      }
      policies = new ArrayList<>(newPolicySet);
      policiesSnapshot = new ArrayList<>(policies);
      implementersSnapshot = new ArrayList<>(implementers);
    }
    for (PolicyImplementer implementer : implementersSnapshot) {
      implementer.onPoliciesChanged(relevantPoliciesFor(implementer, policiesSnapshot));
    }
    return true;
  }

  public void registerImplementer(PolicyImplementer implementer) {
    Objects.requireNonNull(implementer, "implementer cannot be null");
    List<TelemetryPolicy> policiesSnapshot;
    synchronized (this) {
      implementers.add(implementer);
      policiesSnapshot = new ArrayList<>(policies);
    }
    implementer.onPoliciesChanged(relevantPoliciesFor(implementer, policiesSnapshot));
  }

  public synchronized List<TelemetryPolicy> getPolicies() {
    return Collections.unmodifiableList(new ArrayList<>(policies));
  }

  public synchronized void clear() {
    policies = Collections.emptyList();
    implementers.clear();
  }

  private static List<TelemetryPolicy> relevantPoliciesFor(
      PolicyImplementer implementer, List<TelemetryPolicy> policies) {
    Set<String> supportedTypes = new LinkedHashSet<>();
    for (PolicyValidator validator : implementer.getValidators()) {
      if (validator != null && validator.getPolicyType() != null) {
        supportedTypes.add(validator.getPolicyType());
      }
    }
    ArrayList<TelemetryPolicy> relevant = new ArrayList<>();
    for (TelemetryPolicy policy : policies) {
      if (supportedTypes.contains(policy.getType())) {
        relevant.add(policy);
      }
    }
    return Collections.unmodifiableList(relevant);
  }
}
