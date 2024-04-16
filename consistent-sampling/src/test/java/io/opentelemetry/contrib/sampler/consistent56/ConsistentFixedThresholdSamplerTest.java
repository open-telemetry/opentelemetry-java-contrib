/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.TestUtil.generateRandomTraceId;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConsistentFixedThresholdSamplerTest {

  private Context parentContext;
  private String name;
  private SpanKind spanKind;
  private Attributes attributes;
  private List<LinkData> parentLinks;

  @BeforeEach
  public void init() {

    parentContext = Context.root();
    name = "name";
    spanKind = SpanKind.SERVER;
    attributes = Attributes.empty();
    parentLinks = Collections.emptyList();
  }

  private void testSampling(SplittableRandom rng, double samplingProbability) {
    int numSpans = 10000;

    Sampler sampler = ConsistentSampler.probabilityBased(samplingProbability);

    int numSampled = 0;
    for (long i = 0; i < numSpans; ++i) {
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext, generateRandomTraceId(rng), name, spanKind, attributes, parentLinks);
      if (samplingResult.getDecision() == SamplingDecision.RECORD_AND_SAMPLE) {
        String traceStateString =
            samplingResult
                .getUpdatedTraceState(TraceState.getDefault())
                .get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState traceState = OtelTraceState.parse(traceStateString);
        assertThat(traceState.hasValidRandomValue()).isFalse();
        assertThat(traceState.hasValidThreshold()).isTrue();
        assertThat(traceState.getThreshold()).isEqualTo(calculateThreshold(samplingProbability));

        numSampled += 1;
      }
    }

    assertThat(
            new BinomialTest()
                .binomialTest(
                    numSpans, numSampled, samplingProbability, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(0.005);
  }

  @Test
  public void testSampling() {

    // fix seed to get reproducible results
    SplittableRandom random = new SplittableRandom(0);

    testSampling(random, 1.);
    testSampling(random, 0.5);
    testSampling(random, 0.25);
    testSampling(random, 0.125);
    testSampling(random, 0.0);
    testSampling(random, 0.45);
    testSampling(random, 0.2);
    testSampling(random, 0.13);
    testSampling(random, 0.05);
  }

  @Test
  public void testDescription() {
    assertThat(ConsistentSampler.probabilityBased(1.0).getDescription())
        .isEqualTo("ConsistentFixedThresholdSampler{threshold=0, sampling probability=1.0}");
    assertThat(ConsistentSampler.probabilityBased(0.5).getDescription())
        .isEqualTo("ConsistentFixedThresholdSampler{threshold=8, sampling probability=0.5}");
    assertThat(ConsistentSampler.probabilityBased(0.25).getDescription())
        .isEqualTo("ConsistentFixedThresholdSampler{threshold=c, sampling probability=0.25}");
    assertThat(ConsistentSampler.probabilityBased(1e-300).getDescription())
        .isEqualTo("ConsistentFixedThresholdSampler{threshold=max, sampling probability=0.0}");
    assertThat(ConsistentSampler.probabilityBased(0).getDescription())
        .isEqualTo("ConsistentFixedThresholdSampler{threshold=max, sampling probability=0.0}");
  }
}
