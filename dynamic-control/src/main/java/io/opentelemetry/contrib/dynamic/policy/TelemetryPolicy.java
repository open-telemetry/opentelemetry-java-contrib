/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

/**
 * Represents a single telemetry policy with spec-required identity and policy type.
 *
 * <p>Policies are immutable data carriers. Concrete implementations must provide value-based {@code
 * equals} and {@code hashCode} implementations because {@link PolicyStore} deduplicates and detects
 * changes using policy equality.
 *
 * @see io.opentelemetry.contrib.dynamic.policy
 */
public interface TelemetryPolicy {
  /**
   * Returns the identity for this policy instance.
   *
   * <p>The identity distinguishes policy instances within a policy type and is used by {@link
   * PolicyStore} to detect policy removals.
   *
   * @return the non-null policy identity
   */
  TelemetryPolicyIdentity getIdentity();

  /**
   * Returns the policy type.
   *
   * <p>The type identifies the policy behavior and the implementer responsible for applying it, for
   * example {@code trace-sampling}.
   *
   * @return the non-null policy type
   */
  String getType();

  /**
   * Returns whether this policy represents a deleted element.
   *
   * <p>Deleted policies are used to signal explicit removal of a previously known policy. Most
   * policies are not deleted and should leave this method returning false.
   *
   * @return true if this policy represents a deleted element, false otherwise.
   */
  default boolean isDeleted() {
    return false;
  }
}
