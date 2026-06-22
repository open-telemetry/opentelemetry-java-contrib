/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.Objects;

public final class DeletedTelemetryPolicy extends TelemetryPolicy {
  private final TelemetryPolicyIdentity identity;

  public DeletedTelemetryPolicy(TelemetryPolicyIdentity identity, String type) {
    super(type);
    this.identity = Objects.requireNonNull(identity, "identity cannot be null");
  }

  public TelemetryPolicyIdentity getIdentity() {
    return identity;
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
