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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the latest validated policy snapshot and reports whether an update changed effective
 * configuration.
 */
public final class PolicyStore {
  private static final Logger logger = Logger.getLogger(PolicyStore.class.getName());

  private List<TelemetryPolicy> policies = Collections.emptyList();
  private final List<RegisteredImplementer> implementers = new ArrayList<>();
  private long policyVersion;

  /**
   * Replaces the stored policies when the new snapshot is not equal to the current one.
   *
   * <p>Input lists are normalized to a set of distinct policies using value equality: duplicates
   * are dropped and only the first occurrence of each policy is kept (insertion order). Change
   * detection uses set equality, so list order does not matter. That matches telemetry policy
   * semantics where the effective result does not depend on processing order (see the telemetry
   * policy OTEP, commutativity / no user-defined ordering between policies).
   *
   * @return {@code true} if the store was updated, {@code false} if the snapshot was unchanged
   */
  public boolean updatePolicies(List<TelemetryPolicy> newPolicies) {
    Objects.requireNonNull(newPolicies, "newPolicies cannot be null");
    LinkedHashSet<TelemetryPolicy> newPolicySet = new LinkedHashSet<>(newPolicies);
    List<TelemetryPolicy> policiesSnapshot;
    List<TelemetryPolicy> notificationSnapshot;
    List<RegisteredImplementer> implementersSnapshot;
    long snapshotVersion;
    synchronized (this) {
      if (new LinkedHashSet<>(policies).equals(newPolicySet)) {
        return false;
      }
      List<TelemetryPolicy> deletedPolicies =
          deletedPoliciesFrom(policies, new ArrayList<>(newPolicySet));
      policies = new ArrayList<>(newPolicySet);
      policyVersion++;
      snapshotVersion = policyVersion;
      policiesSnapshot = new ArrayList<>(policies);
      notificationSnapshot = new ArrayList<>(deletedPolicies.size() + policiesSnapshot.size());
      notificationSnapshot.addAll(deletedPolicies);
      notificationSnapshot.addAll(policiesSnapshot);
      implementersSnapshot = new ArrayList<>(implementers);
    }
    for (RegisteredImplementer implementer : implementersSnapshot) {
      notifyImplementer(implementer, notificationSnapshot, snapshotVersion);
    }
    return true;
  }

  public void registerImplementer(PolicyImplementer implementer) {
    Objects.requireNonNull(implementer, "implementer cannot be null");
    RegisteredImplementer registeredImplementer = new RegisteredImplementer(implementer);
    List<TelemetryPolicy> policiesSnapshot;
    long snapshotVersion;
    synchronized (this) {
      implementers.add(registeredImplementer);
      snapshotVersion = policyVersion;
      policiesSnapshot = new ArrayList<>(policies);
    }
    notifyImplementer(registeredImplementer, policiesSnapshot, snapshotVersion);
  }

  public synchronized List<TelemetryPolicy> getPolicies() {
    return Collections.unmodifiableList(new ArrayList<>(policies));
  }

  public synchronized void clear() {
    policies = Collections.emptyList();
    implementers.clear();
    policyVersion = 0;
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

  private static List<TelemetryPolicy> deletedPoliciesFrom(
      List<TelemetryPolicy> previousPolicies, List<TelemetryPolicy> newPolicies) {
    Set<String> activePolicyKeys = new LinkedHashSet<>();
    for (TelemetryPolicy policy : newPolicies) {
      activePolicyKeys.add(policyKey(policy));
    }
    ArrayList<TelemetryPolicy> deletedPolicies = new ArrayList<>();
    for (TelemetryPolicy previousPolicy : previousPolicies) {
      String policyKey = policyKey(previousPolicy);
      TelemetryPolicyIdentity identity = previousPolicy.getIdentity();
      if (!activePolicyKeys.contains(policyKey)) {
        deletedPolicies.add(new DeletedTelemetryPolicy(identity, previousPolicy.getType()));
      }
    }
    return deletedPolicies;
  }

  private static String policyKey(TelemetryPolicy policy) {
    return policy.getType() + "\u0000" + policy.getIdentity().getId();
  }

  private static void notifyImplementer(
      RegisteredImplementer registration, List<TelemetryPolicy> policiesSnapshot, long version) {
    synchronized (registration) {
      if (version <= registration.lastDeliveredVersion) {
        return;
      }
      try {
        registration.implementer.onPoliciesChanged(
            relevantPoliciesFor(registration.implementer, policiesSnapshot));
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Policy implementer failed to apply policy update", e);
      } finally {
        registration.lastDeliveredVersion = version;
      }
    }
  }

  private static final class RegisteredImplementer {
    private final PolicyImplementer implementer;
    private long lastDeliveredVersion = -1;

    private RegisteredImplementer(PolicyImplementer implementer) {
      this.implementer = implementer;
    }
  }
}
