/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Top-level registry initialization model containing per-source mapping config. */
public final class PolicyInitConfig {

  private final List<PolicySourceConfig> sources;

  public PolicyInitConfig(List<PolicySourceConfig> sources) {
    List<PolicySourceConfig> sourceCopy =
        new ArrayList<>(Objects.requireNonNull(sources, "sources cannot be null"));
    for (PolicySourceConfig source : sourceCopy) {
      Objects.requireNonNull(source, "sources cannot contain null elements");
    }
    this.sources = Collections.unmodifiableList(sourceCopy);
  }

  public List<PolicySourceConfig> getSources() {
    return sources;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PolicyInitConfig)) {
      return false;
    }
    PolicyInitConfig that = (PolicyInitConfig) obj;
    return sources.equals(that.sources);
  }

  @Override
  public int hashCode() {
    return sources.hashCode();
  }
}
