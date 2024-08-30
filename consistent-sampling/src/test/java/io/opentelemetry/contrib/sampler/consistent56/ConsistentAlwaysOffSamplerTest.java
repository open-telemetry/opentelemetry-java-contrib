/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
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
    assertThat(
            ConsistentSampler.alwaysOff()
                .getSamplingIntent(null, "span_name", null, null, null)
                .getThreshold())
        .isEqualTo(getInvalidThreshold());
  }
}
