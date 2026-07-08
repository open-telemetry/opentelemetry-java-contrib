/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeletedTelemetryPolicyTest {

  @Test
  void storesIdentityTypeAndDeletedState() {
    TelemetryPolicyIdentity identity =
        new TelemetryPolicyIdentity("trace-sampling", "Trace sampling rate");
    DeletedTelemetryPolicy policy = new DeletedTelemetryPolicy(identity, "trace-sampling");

    assertThat(policy.getIdentity()).isEqualTo(identity);
    assertThat(policy.getType()).isEqualTo("trace-sampling");
    assertThat(policy.isDeleted()).isTrue();
  }

  @Test
  void ordinaryTelemetryPolicyIsNotDeleted() {
    assertThat(new TestTelemetryPolicy("trace-sampling").isDeleted()).isFalse();
  }

  private static final class TestTelemetryPolicy implements TelemetryPolicy {
    private final TelemetryPolicyIdentity identity;
    private final String type;

    private TestTelemetryPolicy(String type) {
      this.identity = new TelemetryPolicyIdentity(type, "Test policy");
      this.type = type;
    }

    @Override
    public TelemetryPolicyIdentity getIdentity() {
      return identity;
    }

    @Override
    public String getType() {
      return type;
    }
  }
}
