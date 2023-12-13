/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMinThreshold;
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
        .isEqualTo(getMinThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getInvalidThreshold(), true))
        .isEqualTo(getMinThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getMinThreshold(), false))
        .isEqualTo(getMinThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(getMinThreshold(), true))
        .isEqualTo(getMinThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(0, false)).isEqualTo(getMinThreshold());
    assertThat(ConsistentSampler.alwaysOn().getThreshold(0, true)).isEqualTo(getMinThreshold());
  }
}
