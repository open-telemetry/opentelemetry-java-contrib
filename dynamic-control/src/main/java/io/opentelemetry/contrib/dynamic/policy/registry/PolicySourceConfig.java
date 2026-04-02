/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** One configured policy source with source-local key-to-policy mappings. */
public final class PolicySourceConfig {

  private final SourceKind kind;
  private final SourceFormat format;
  @Nullable private final String location;
  private final List<PolicySourceMappingConfig> mappings;

  public PolicySourceConfig(
      SourceKind kind,
      SourceFormat format,
      @Nullable String location,
      List<PolicySourceMappingConfig> mappings) {
    this.kind = Objects.requireNonNull(kind, "kind cannot be null");
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.location = location;
    List<PolicySourceMappingConfig> mappingCopy =
        new ArrayList<>(Objects.requireNonNull(mappings, "mappings cannot be null"));
    for (PolicySourceMappingConfig mapping : mappingCopy) {
      Objects.requireNonNull(mapping, "mappings cannot contain null elements");
    }
    this.mappings = Collections.unmodifiableList(mappingCopy);
  }

  public SourceKind getKind() {
    return kind;
  }

  public SourceFormat getFormat() {
    return format;
  }

  @Nullable
  public String getLocation() {
    return location;
  }

  public List<PolicySourceMappingConfig> getMappings() {
    return mappings;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PolicySourceConfig)) {
      return false;
    }
    PolicySourceConfig that = (PolicySourceConfig) obj;
    return kind == that.kind
        && format == that.format
        && Objects.equals(location, that.location)
        && mappings.equals(that.mappings);
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + format.hashCode();
    result = 31 * result + (location == null ? 0 : location.hashCode());
    result = 31 * result + mappings.hashCode();
    return result;
  }
}
