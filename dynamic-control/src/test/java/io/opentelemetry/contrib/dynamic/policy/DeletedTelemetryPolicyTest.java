/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import org.junit.jupiter.api.Test;

class DeletedTelemetryPolicyTest {

  @Test
  void storesIdentityTypeAndDeletedState() {
    TelemetryPolicyIdentity identity =
        new TelemetryPolicyIdentity("trace-sampling", "Trace sampling rate");
    DeletedTelemetryPolicy policy =
        new DeletedTelemetryPolicy(identity, "trace-sampling", SourceKind.CUSTOM);

    assertThat(policy.getIdentity()).isEqualTo(identity);
    assertThat(policy.getType()).isEqualTo("trace-sampling");
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.CUSTOM);
    assertThat(policy.isDeleted()).isTrue();
  }

  @Test
  void storesExplicitSourceKind() {
    TelemetryPolicyIdentity identity =
        new TelemetryPolicyIdentity("trace-sampling", "Trace sampling rate");
    DeletedTelemetryPolicy policy =
        new DeletedTelemetryPolicy(identity, "trace-sampling", SourceKind.OPAMP);

    assertThat(policy.getIdentity()).isEqualTo(identity);
    assertThat(policy.getType()).isEqualTo("trace-sampling");
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.OPAMP);
  }

  @Test
  void ordinaryTelemetryPolicyIsNotDeleted() {
    assertThat(new TestTelemetryPolicy("trace-sampling").isDeleted()).isFalse();
  }

  private static final class TestTelemetryPolicy implements TelemetryPolicy {
    private final TelemetryPolicyIdentity identity;
    private final String type;
    private final SourceKind sourceKind;

    private TestTelemetryPolicy(String type) {
      this(type, SourceKind.CUSTOM);
    }

    private TestTelemetryPolicy(String type, SourceKind sourceKind) {
      this.identity = new TelemetryPolicyIdentity(type, "Test policy");
      this.type = type;
      this.sourceKind = sourceKind;
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
  }
}
