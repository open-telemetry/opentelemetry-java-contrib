/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.Objects;

/**
 * Represents a single Telemetry Policy identified by type.
 *
 * <p>A {@code TelemetryPolicy} instance encapsulates a specific rule or configuration intent
 * identified by its {@link #getType() type}.
 *
 * <p>Policies are immutable data carriers.
 *
 * <p>As an example, policy type {@code trace-sampling} indicates that trace sampling behavior
 * should be configured.
 *
 * <p>Direct instantiation of this base class is intentionally supported for type-only policy
 * signals (for example, to indicate policy removal/reset without policy-specific values).
 *
 * @see io.opentelemetry.contrib.dynamic.policy
 */
public class TelemetryPolicy {
  private final String type; // e.g. "trace-sampling"

  /**
   * Constructs a new TelemetryPolicy.
   *
   * <p>This constructor is used by type-specific subclasses and also directly for type-only policy
   * signals such as policy removal/reset.
   *
   * @param type the type of the policy (e.g., "trace-sampling"), must not be null.
   */
  public TelemetryPolicy(String type) {
    Objects.requireNonNull(type, "type cannot be null");
    this.type = type;
  }

  /**
   * Returns the type of this policy.
   *
   * <p>The type identifies the domain and expected behavior of the policy (e.g., "trace-sampling",
   * "metric-rate").
   *
   * @return the policy type.
   */
  public String getType() {
    return type;
  }

  /**
   * Type-only policies ({@link TelemetryPolicy} instances) do not equal typed subclasses that share
   * the same {@link #getType() type} string.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TelemetryPolicy)) {
      return false;
    }
    TelemetryPolicy that = (TelemetryPolicy) obj;
    if (that.getClass() != TelemetryPolicy.class || getClass() != TelemetryPolicy.class) {
      return false;
    }
    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type);
  }
}
