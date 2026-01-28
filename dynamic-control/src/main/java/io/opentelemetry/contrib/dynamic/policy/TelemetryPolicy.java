/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import javax.annotation.Nullable;

public class TelemetryPolicy {
  private final String type; // e.g. "trace-sampling"
  // JSON content after schema validation
  // null means removed, which is relevant for merging policies
  @Nullable private final JsonNode spec;

  public TelemetryPolicy(String type, @Nullable JsonNode spec) {
    this.type = type;
    this.spec = spec;
  }

  public String getType() {
    return type;
  }

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
