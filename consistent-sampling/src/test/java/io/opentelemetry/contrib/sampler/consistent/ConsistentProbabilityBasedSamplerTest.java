/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.hipparchus.stat.inference.GTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConsistentProbabilityBasedSamplerTest {

  private Context parentContext;
  private String traceId;
  private String name;
  private SpanKind spanKind;
  private Attributes attributes;
  private List<LinkData> parentLinks;

  @BeforeEach
  public void init() {

    parentContext = Context.root();
    traceId = "0123456789abcdef0123456789abcdef";
    name = "name";
    spanKind = SpanKind.SERVER;
    attributes = Attributes.empty();
    parentLinks = Collections.emptyList();
  }

  private void test(SplittableRandom rng, double samplingProbability) {
    int numSpans = 1000000;

    Sampler sampler =
        ConsistentSampler.probabilityBased(
            samplingProbability,
            s -> RandomGenerator.create(rng::nextLong).numberOfLeadingZerosOfRandomLong());

    Map<Integer, Long> observedPvalues = new HashMap<>();
    for (long i = 0; i < numSpans; ++i) {
      SamplingResult samplingResult =
          sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      if (samplingResult.getDecision() == SamplingDecision.RECORD_AND_SAMPLE) {
        String traceStateString =
            samplingResult
                .getUpdatedTraceState(TraceState.getDefault())
                .get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState traceState = OtelTraceState.parse(traceStateString);
        assertTrue(traceState.hasValidR());
        assertTrue(traceState.hasValidP());
        observedPvalues.merge(traceState.getP(), 1L, Long::sum);
      }
    }
    verifyObservedPvaluesUsingGtest(numSpans, observedPvalues, samplingProbability);
  }

  @Test
  public void test() {

    // fix seed to get reproducible results
    SplittableRandom random = new SplittableRandom(0);

    test(random, 1.);
    test(random, 0.5);
    test(random, 0.25);
    test(random, 0.125);
    test(random, 0.0);
    test(random, 0.45);
    test(random, 0.2);
    test(random, 0.13);
    test(random, 0.05);
  }

  private static void verifyObservedPvaluesUsingGtest(
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
