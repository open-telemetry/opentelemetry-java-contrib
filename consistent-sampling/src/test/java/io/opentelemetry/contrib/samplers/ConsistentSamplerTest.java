/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.contrib.state.OtelTraceState;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class ConsistentSamplerTest {

  @Test
  void testGetSamplingRate() {
    assertEquals(Double.NaN, ConsistentSampler.getSamplingProbability(-1));
    for (int i = 0; i < OtelTraceState.getMaxP() - 1; i += 1) {
      assertEquals(Math.pow(0.5, i), ConsistentSampler.getSamplingProbability(i));
    }
    assertEquals(0., ConsistentSampler.getSamplingProbability(OtelTraceState.getMaxP()));
    assertEquals(
        Double.NaN, ConsistentSampler.getSamplingProbability(OtelTraceState.getMaxP() + 1));
  }

  @Test
  void testGetLowerBoundP() {
    assertEquals(0, ConsistentSampler.getLowerBoundP(1.0));
    assertEquals(0, ConsistentSampler.getLowerBoundP(Math.nextDown(1.0)));
    for (int i = 1; i < OtelTraceState.getMaxP() - 1; i += 1) {
      double samplingProbability = Math.pow(0.5, i);
      assertEquals(i, ConsistentSampler.getLowerBoundP(samplingProbability));
      assertEquals(i - 1, ConsistentSampler.getLowerBoundP(Math.nextUp(samplingProbability)));
      assertEquals(i, ConsistentSampler.getLowerBoundP(Math.nextDown(samplingProbability)));
    }
    assertEquals(OtelTraceState.getMaxP() - 1, ConsistentSampler.getLowerBoundP(Double.MIN_NORMAL));
    assertEquals(OtelTraceState.getMaxP() - 1, ConsistentSampler.getLowerBoundP(Double.MIN_VALUE));
    assertEquals(OtelTraceState.getMaxP(), ConsistentSampler.getLowerBoundP(0.0));
  }

  @Test
  void testGetUpperBoundP() {
    assertEquals(0, ConsistentSampler.getUpperBoundP(1.0));
    assertEquals(1, ConsistentSampler.getUpperBoundP(Math.nextDown(1.0)));
    for (int i = 1; i < OtelTraceState.getMaxP() - 1; i += 1) {
      double samplingProbability = Math.pow(0.5, i);
      assertEquals(i, ConsistentSampler.getUpperBoundP(samplingProbability));
      assertEquals(i, ConsistentSampler.getUpperBoundP(Math.nextUp(samplingProbability)));
      assertEquals(i + 1, ConsistentSampler.getUpperBoundP(Math.nextDown(samplingProbability)));
    }
    assertEquals(OtelTraceState.getMaxP(), ConsistentSampler.getUpperBoundP(Double.MIN_NORMAL));
    assertEquals(OtelTraceState.getMaxP(), ConsistentSampler.getUpperBoundP(Double.MIN_VALUE));
    assertEquals(OtelTraceState.getMaxP(), ConsistentSampler.getUpperBoundP(0.0));
  }

  @Test
  void testRandomValues() {
    int numCycles = 1000;
    SplittableRandom random = new SplittableRandom(0L);
    for (int i = 0; i < numCycles; ++i) {
      double samplingProbability = Math.exp(-1. / random.nextDouble());
      int pmin = ConsistentSampler.getLowerBoundP(samplingProbability);
      int pmax = ConsistentSampler.getUpperBoundP(samplingProbability);
      assertThat(ConsistentSampler.getSamplingProbability(pmin))
          .isGreaterThanOrEqualTo(samplingProbability);
      assertThat(ConsistentSampler.getSamplingProbability(pmax))
          .isLessThanOrEqualTo(samplingProbability);
    }
  }
}
