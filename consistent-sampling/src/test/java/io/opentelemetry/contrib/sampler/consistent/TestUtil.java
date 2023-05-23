/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.hipparchus.stat.inference.GTest;

public final class TestUtil {

  private TestUtil() {}

  public static void verifyObservedPvaluesUsingGtest(
      long originalNumberOfSpans, Map<Integer, Long> observedPvalues, double samplingProbability) {

    Object notSampled =
        new Object() {
          @Override
          public String toString() {
            return "NOT SAMPLED";
          }
        };

    Map<Object, Double> expectedProbabilities = new HashMap<>();
    if (samplingProbability >= 1.) {
      expectedProbabilities.put(0, 1.);
    } else if (samplingProbability <= 0.) {
      expectedProbabilities.put(notSampled, 1.);
    } else {
      int exponent = 0;
      while (true) {
        if (Math.pow(0.5, exponent + 1) < samplingProbability
            && Math.pow(0.5, exponent) >= samplingProbability) {
          break;
        }
        exponent += 1;
      }
      if (samplingProbability == Math.pow(0.5, exponent)) {
        expectedProbabilities.put(notSampled, 1 - samplingProbability);
        expectedProbabilities.put(exponent, samplingProbability);
      } else {
        expectedProbabilities.put(notSampled, 1 - samplingProbability);
        expectedProbabilities.put(exponent, 2 * samplingProbability - Math.pow(0.5, exponent));
        expectedProbabilities.put(exponent + 1, Math.pow(0.5, exponent) - samplingProbability);
      }
    }

    Map<Object, Long> extendedObservedAdjustedCounts = new HashMap<>(observedPvalues);
    long numberOfSpansNotSampled =
        originalNumberOfSpans - observedPvalues.values().stream().mapToLong(i -> i).sum();
    if (numberOfSpansNotSampled > 0) {
      extendedObservedAdjustedCounts.put(notSampled, numberOfSpansNotSampled);
    }

    double[] expectedValues = new double[expectedProbabilities.size()];
    long[] observedValues = new long[expectedProbabilities.size()];

    int counter = 0;
    for (Object key : expectedProbabilities.keySet()) {
      observedValues[counter] = extendedObservedAdjustedCounts.getOrDefault(key, 0L);
      double p = expectedProbabilities.get(key);
      expectedValues[counter] = p * originalNumberOfSpans;
      counter += 1;
    }

    if (expectedProbabilities.size() > 1) {
      assertThat(new GTest().gTest(expectedValues, observedValues)).isGreaterThan(0.01);
    } else {
      assertThat((double) observedValues[0]).isEqualTo(expectedValues[0]);
    }
  }
}
