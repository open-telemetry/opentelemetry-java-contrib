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

/**
 * Holds the latest validated policy snapshot and reports whether an update changed effective
 * configuration.
 */
public final class PolicyStore {

  private List<TelemetryPolicy> policies = Collections.emptyList();

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
  public synchronized boolean updatePolicies(List<TelemetryPolicy> newPolicies) {
    Objects.requireNonNull(newPolicies, "newPolicies cannot be null");
    List<TelemetryPolicy> normalized = new ArrayList<>(new LinkedHashSet<>(newPolicies));
    if (new LinkedHashSet<>(policies).equals(new LinkedHashSet<>(normalized))) {
      return false;
    }
    policies = normalized;
    return true;
  }

  public synchronized List<TelemetryPolicy> getPolicies() {
    return Collections.unmodifiableList(new ArrayList<>(policies));
  }
}
