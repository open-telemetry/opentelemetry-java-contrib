/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.Objects;

public final class DeletedTelemetryPolicy implements TelemetryPolicy {
  private final TelemetryPolicyIdentity identity;
  private final String type;

  public DeletedTelemetryPolicy(TelemetryPolicyIdentity identity, String type) {
    this.identity = Objects.requireNonNull(identity, "identity cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
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
  public boolean isDeleted() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DeletedTelemetryPolicy)) {
      return false;
    }
    DeletedTelemetryPolicy that = (DeletedTelemetryPolicy) obj;
    return identity.equals(that.identity) && getType().equals(that.getType());
  }

  @Override
  public int hashCode() {
    int result = identity.hashCode();
    result = 31 * result + getType().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "DeletedTelemetryPolicy{identity=" + identity + ", type='" + getType() + "'}";
  }
}
