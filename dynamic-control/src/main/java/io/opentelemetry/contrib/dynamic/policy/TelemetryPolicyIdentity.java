/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Objects;

public final class TelemetryPolicyIdentity {
  private final String id;
  private final String name;

  public TelemetryPolicyIdentity(String id, String name) {
    this.id = requireNonBlank(id, "id");
    this.name = requireNonBlank(name, "name");
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @CanIgnoreReturnValue
  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " cannot be null");
    if (value.trim().isEmpty()) {
      throw new IllegalArgumentException("Field '" + fieldName + "' must be non-empty.");
    }
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TelemetryPolicyIdentity)) {
      return false;
    }
    TelemetryPolicyIdentity that = (TelemetryPolicyIdentity) obj;
    return id.equals(that.id) && name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "TelemetryPolicyIdentity{id='" + id + "', name='" + name + "'}";
  }
}
