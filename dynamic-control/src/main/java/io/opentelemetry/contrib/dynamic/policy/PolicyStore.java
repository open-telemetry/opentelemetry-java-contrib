/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Holds the latest validated policy snapshot and publishes accepted updates to implementers. */
public final class PolicyStore {
  private static final Logger logger = Logger.getLogger(PolicyStore.class.getName());

  private List<TelemetryPolicy> policies = Collections.emptyList();
  private final List<RegisteredImplementer> implementers = new ArrayList<>();
  private long policyVersion;

  /**
   * Replaces the stored policies with a new accepted snapshot.
   *
   * <p>Input lists are normalized by policy identity, using policy type and policy id. Duplicate
   * policies with the same identity are dropped and only the first occurrence is kept in insertion
   * order. The store does not perform value-level no-op detection; providers can use source
   * hashes/versions to skip unchanged snapshots, and implementers must apply policies idempotently.
   *
   * @return {@code true} after the snapshot is accepted
   */
  public boolean updatePolicies(List<TelemetryPolicy> newPolicies) {
    Objects.requireNonNull(newPolicies, "newPolicies cannot be null");
    Map<PolicyKey, TelemetryPolicy> newPolicyMap = normalizedPolicyMap(newPolicies);
    List<TelemetryPolicy> policiesSnapshot;
    List<TelemetryPolicy> notificationSnapshot;
    List<RegisteredImplementer> implementersSnapshot;
    long snapshotVersion;
    synchronized (this) {
      List<TelemetryPolicy> deletedPolicies = deletedPoliciesFrom(policies, newPolicyMap.keySet());
      policies = new ArrayList<>(newPolicyMap.values());
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
      List<TelemetryPolicy> previousPolicies, Set<PolicyKey> activePolicyKeys) {
    ArrayList<TelemetryPolicy> deletedPolicies = new ArrayList<>();
    for (TelemetryPolicy previousPolicy : previousPolicies) {
      PolicyKey policyKey = policyKey(previousPolicy);
      TelemetryPolicyIdentity identity = previousPolicy.getIdentity();
      if (!activePolicyKeys.contains(policyKey)) {
        deletedPolicies.add(new DeletedTelemetryPolicy(identity, previousPolicy.getType()));
      }
    }
    return deletedPolicies;
  }

  private static Map<PolicyKey, TelemetryPolicy> normalizedPolicyMap(
      List<TelemetryPolicy> policies) {
    LinkedHashMap<PolicyKey, TelemetryPolicy> normalized = new LinkedHashMap<>();
    for (TelemetryPolicy policy : policies) {
      Objects.requireNonNull(policy, "newPolicies cannot contain null elements");
      PolicyKey key = policyKey(policy);
      if (!normalized.containsKey(key)) {
        normalized.put(key, policy);
      }
    }
    return normalized;
  }

  private static PolicyKey policyKey(TelemetryPolicy policy) {
    return new PolicyKey(policy.getType(), policy.getIdentity().getId());
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

  private static final class PolicyKey {
    private final String type;
    private final String id;

    private PolicyKey(String type, String id) {
      this.type = Objects.requireNonNull(type, "policy type cannot be null");
      this.id = Objects.requireNonNull(id, "policy id cannot be null");
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof PolicyKey)) {
        return false;
      }
      PolicyKey that = (PolicyKey) obj;
      return type.equals(that.type) && id.equals(that.id);
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + id.hashCode();
      return result;
    }
  }
}
