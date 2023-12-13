/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxThreshold;
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
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getInvalidThreshold(), false))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getInvalidThreshold(), true))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getMaxThreshold(), false))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOff().getThreshold(getMaxThreshold(), true))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOff().getThreshold(0, false)).isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOff().getThreshold(0, true)).isEqualTo(getMaxThreshold());
  }
}
