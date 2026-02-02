/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents a single Telemetry Policy, comprising a type and a specification.
 *
 * <p>A {@code TelemetryPolicy} instance encapsulates a specific rule or configuration intent
 * identified by its {@link #getType() type}. The behavior of the policy is defined by its {@link
 * #getSpec() specification}, which is a JSON object.
 *
 * <p>Policies are typically immutable data carriers. The {@code spec} can be {@code null}, which
 * conventionally signifies the removal or absence of a policy for the given type in certain
 * contexts (e.g., when calculating diffs or updates).
 *
 * <p>As an example take the JSON structure `{"trace-sampling": {"probability" : 0.5}}` This is of
 * type "trace-sampling", with spec `{"probability" : 0.5}`, indicating the intent that the trace
 * sampling-probability be set to 50%
 *
 * @see io.opentelemetry.contrib.dynamic.policy
 */
public class TelemetryPolicy {
  private final String type; // e.g. "trace-sampling"
  // JSON content after schema validation
  // null means removed, which is relevant for merging policies
  @Nullable private final JsonNode spec;

  /**
   * Constructs a new TelemetryPolicy.
   *
   * @param type the type of the policy (e.g., "trace-sampling"), must not be null.
   * @param spec the JSON specification of the policy, or {@code null} to indicate removal.
   */
  public TelemetryPolicy(String type, @Nullable JsonNode spec) {
    Objects.requireNonNull(type, "type cannot be null");
    this.type = type;
    this.spec = spec;
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
   * Returns the specification of this policy.
   *
   * <p>The specification is a JSON structure defining the parameters of the policy. If {@code
   * null}, it may indicate that the policy is being removed or is empty.
   *
   * @return the policy specification, or {@code null}.
   */
  @Nullable
  public JsonNode getSpec() {
    return spec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TelemetryPolicy)) {
      return false;
    }
    TelemetryPolicy that = (TelemetryPolicy) o;
    return Objects.equals(type, that.type) && Objects.equals(spec, that.spec);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, spec);
  }
}
