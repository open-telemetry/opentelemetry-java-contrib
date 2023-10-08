/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent2;

import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getMaxRandomValue;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RandomValueGeneratorsTest {
  @Test
  void testRandomRange() {
    int attempts = 10000;
    for (int i = 0; i < attempts; ++i) {
      assertThat(RandomValueGenerators.getDefault().generate(""))
          .isBetween(0L, getMaxRandomValue());
    }
  }
}
