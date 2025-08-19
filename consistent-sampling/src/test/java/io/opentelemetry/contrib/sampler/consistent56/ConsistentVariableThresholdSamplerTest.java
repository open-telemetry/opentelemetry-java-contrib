/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxThreshold;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConsistentVariableThresholdSamplerTest {

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
    String thresholdString =
        ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                new StringBuilder(), threshold)
            .toString();
    if (threshold == getMaxThreshold()) {
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
