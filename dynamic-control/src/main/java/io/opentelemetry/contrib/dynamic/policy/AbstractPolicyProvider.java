/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Base {@link PolicyProvider} implementation for providers that maintain a current snapshot. */
abstract class AbstractPolicyProvider implements PolicyProvider {
  private final AtomicReference<List<TelemetryPolicy>> currentPolicies =
      new AtomicReference<>(Collections.<TelemetryPolicy>emptyList());

  protected final List<TelemetryPolicy> getCurrentPolicies() {
    return Objects.requireNonNull(currentPolicies.get(), "currentPolicies cannot be null");
  }

  protected final List<TelemetryPolicy> updateCurrentPolicies(List<TelemetryPolicy> policies) {
    List<TelemetryPolicy> snapshot =
        Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(policies, "policies cannot be null")));
    currentPolicies.set(snapshot);
    return snapshot;
  }

  protected final List<TelemetryPolicy> updateCurrentPoliciesAndNotify(
      List<TelemetryPolicy> policies, Consumer<List<TelemetryPolicy>> onUpdate) {
    Objects.requireNonNull(onUpdate, "onUpdate cannot be null");
    List<TelemetryPolicy> snapshot = updateCurrentPolicies(policies);
    onUpdate.accept(snapshot);
    return snapshot;
  }
}
