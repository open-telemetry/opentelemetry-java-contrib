/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.util.Objects;

public final class DeletedTelemetryPolicy implements TelemetryPolicy {
  private final TelemetryPolicyIdentity identity;
  private final String type;
  private final SourceKind sourceKind;

  // TODO after "source" prioritization handling is complete, remove this constructor
  public DeletedTelemetryPolicy(TelemetryPolicyIdentity identity, String type) {
    this(identity, type, SourceKind.CUSTOM);
  }

  public DeletedTelemetryPolicy(
      TelemetryPolicyIdentity identity, String type, SourceKind sourceKind) {
    this.identity = Objects.requireNonNull(identity, "identity cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.sourceKind = Objects.requireNonNull(sourceKind, "sourceKind cannot be null");
  }

  @Override
  public TelemetryPolicyIdentity getIdentity() {
    return identity;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public SourceKind getSourceKind() {
    return sourceKind;
  }

  @Override
  public boolean isDeleted() {
    return true;
  }

  @Override
  public String toString() {
    return "DeletedTelemetryPolicy{identity="
        + identity
        + ", type='"
        + getType()
        + "', sourceKind="
        + sourceKind
        + "}";
  }
}
