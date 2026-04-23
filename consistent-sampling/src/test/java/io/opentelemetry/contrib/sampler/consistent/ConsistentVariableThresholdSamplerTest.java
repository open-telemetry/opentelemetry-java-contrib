/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.encodeLast56BitHexWithoutTrailingZeros;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConsistentVariableThresholdSamplerTest {

  private static final long MAX_THRESHOLD = 1L << 56;

  @Test
  void testSetSamplingProbability() {
    double probability = 0.5;
    ConsistentVariableThresholdSampler sampler =
        new ConsistentVariableThresholdSampler(probability);
    testSetSamplingProbability(probability, sampler, /* updateProbability= */ false);
    testSetSamplingProbability(0.25, sampler, /* updateProbability= */ true);
    testSetSamplingProbability(0.0, sampler, /* updateProbability= */ true);
    testSetSamplingProbability(1.0, sampler, /* updateProbability= */ true);
  }

  private static void testSetSamplingProbability(
      double probability, ConsistentVariableThresholdSampler sampler, boolean updateProbability) {
    long threshold = calculateThreshold(probability);
    String thresholdString = encodeLast56BitHexWithoutTrailingZeros(threshold);
    if (threshold == MAX_THRESHOLD) {
      thresholdString = "max";
    }
    if (updateProbability) {
      sampler.setSamplingProbability(probability);
    }
    assertThat(sampler.getThreshold()).isEqualTo(threshold);
    assertThat(sampler.getDescription())
        .isEqualTo(
            "ConsistentVariableThresholdSampler{threshold="
                + thresholdString
                + ", sampling probability="
                + probability
                + "}");
  }
}
