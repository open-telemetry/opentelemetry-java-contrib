/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds the latest validated policy snapshot and reports whether an update changed effective
 * configuration.
 */
public final class PolicyStore {

  private volatile List<TelemetryPolicy> policies = Collections.emptyList();

  /**
   * Replaces the stored policies when the new list is not equal to the current snapshot.
   *
   * @return {@code true} if the store was updated, {@code false} if the list was equal
   */
  public synchronized boolean updatePolicies(List<TelemetryPolicy> newPolicies) {
    Objects.requireNonNull(newPolicies, "newPolicies cannot be null");
    List<TelemetryPolicy> copy = new ArrayList<>(newPolicies);
    if (policies.equals(copy)) {
      return false;
    }
    policies = copy;
    return true;
  }

  public synchronized List<TelemetryPolicy> getPolicies() {
    return Collections.unmodifiableList(new ArrayList<>(policies));
  }
}
