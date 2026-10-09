/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import java.util.Objects;

/** One policy mapping from policy ID to target policy type. */
public final class PolicySourceMappingConfig {

  private final String policyId;
  private final String policyType;

  public PolicySourceMappingConfig(String policyId, String policyType) {
    this.policyId = Objects.requireNonNull(policyId, "policyId cannot be null");
    this.policyType = Objects.requireNonNull(policyType, "policyType cannot be null");
  }

  public String getPolicyId() {
    return policyId;
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
    return policyId.equals(that.policyId) && policyType.equals(that.policyType);
  }

  @Override
  public int hashCode() {
    int result = policyId.hashCode();
    result = 31 * result + policyType.hashCode();
    return result;
  }
}
