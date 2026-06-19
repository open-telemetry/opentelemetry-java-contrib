/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import java.util.Objects;

/** One source-local mapping from source key to target policy type. */
public final class PolicySourceMappingConfig {

  private final String sourceKey;
  private final String policyType;

  public PolicySourceMappingConfig(String sourceKey, String policyType) {
    this.sourceKey = Objects.requireNonNull(sourceKey, "sourceKey cannot be null");
    this.policyType = Objects.requireNonNull(policyType, "policyType cannot be null");
  }

  public String getSourceKey() {
    return sourceKey;
  }

  public String getPolicyType() {
    return policyType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PolicySourceMappingConfig)) {
      return false;
    }
    PolicySourceMappingConfig that = (PolicySourceMappingConfig) obj;
    return sourceKey.equals(that.sourceKey) && policyType.equals(that.policyType);
  }

  @Override
  public int hashCode() {
    int result = sourceKey.hashCode();
    result = 31 * result + policyType.hashCode();
    return result;
  }
}
