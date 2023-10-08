/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent2;

import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getMaxThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ConsistentAlwaysOffSamplerTest {

  @Test
  void testDescription() {
    assertThat(ConsistentSampler.alwaysOff().getDescription())
        .isEqualTo("ConsistentAlwaysOffSampler");
  }

  @Test
  void testThreshold() {
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getInvalidThreshold(), false)).isZero();
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getInvalidThreshold(), true)).isZero();
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getMaxThreshold(), false)).isZero();
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getMaxThreshold(), true)).isZero();
    assertThat(ConsistentSampler.alwaysOff().getThreshold(0, false)).isZero();
    assertThat(ConsistentSampler.alwaysOff().getThreshold(0, true)).isZero();
  }
}
