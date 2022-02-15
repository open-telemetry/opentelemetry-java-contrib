/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static io.opentelemetry.contrib.util.TestUtil.verifyObservedPvaluesUsingGtest;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.state.OtelTraceState;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
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

    Sampler sampler = ConsistentSampler.probabilityBased(samplingProbability, rng::nextLong);

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
}
