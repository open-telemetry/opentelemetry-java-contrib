/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ConsistentAlwaysOnSamplerTest {

  @Test
  void testDescription() {
    assertThat(ConsistentSampler.alwaysOn().getDescription())
        .isEqualTo("ConsistentAlwaysOnSampler");
  }

  @Test
  void testThreshold() {
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getInvalidThreshold(), false))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getInvalidThreshold(), true))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getMaxThreshold(), false))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getMaxThreshold(), true))
        .isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(0, false)).isEqualTo(getMaxThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(0, true)).isEqualTo(getMaxThreshold());
  }
}
