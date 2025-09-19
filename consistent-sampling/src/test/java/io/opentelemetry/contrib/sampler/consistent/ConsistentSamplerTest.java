/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.OtelTraceState.getInvalidP;
import static io.opentelemetry.contrib.sampler.consistent.OtelTraceState.getInvalidR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class ConsistentSamplerTest {

  @Test
  void testGetSamplingRate() {
    assertThatThrownBy(() -> ConsistentSampler.getSamplingProbability(-1))
        .isInstanceOf(IllegalArgumentException.class);
    for (int i = 0; i < OtelTraceState.getMaxP() - 1; i += 1) {
      assertThat(ConsistentSampler.getSamplingProbability(i)).isEqualTo(Math.pow(0.5, i));
    }
    assertThat(ConsistentSampler.getSamplingProbability(OtelTraceState.getMaxP())).isEqualTo(0.);
    assertThatThrownBy(() -> ConsistentSampler.getSamplingProbability(OtelTraceState.getMaxP() + 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testGetLowerBoundP() {
    assertThat(ConsistentSampler.getLowerBoundP(1.0)).isEqualTo(0);
    assertThat(ConsistentSampler.getLowerBoundP(Math.nextDown(1.0))).isEqualTo(0);
    for (int i = 1; i < OtelTraceState.getMaxP() - 1; i += 1) {
      double samplingProbability = Math.pow(0.5, i);
      assertThat(ConsistentSampler.getLowerBoundP(samplingProbability)).isEqualTo(i);
      assertThat(ConsistentSampler.getLowerBoundP(Math.nextUp(samplingProbability)))
          .isEqualTo(i - 1);
      assertThat(ConsistentSampler.getLowerBoundP(Math.nextDown(samplingProbability))).isEqualTo(i);
    }
    assertThat(ConsistentSampler.getLowerBoundP(Double.MIN_NORMAL))
        .isEqualTo(OtelTraceState.getMaxP() - 1);
    assertThat(ConsistentSampler.getLowerBoundP(Double.MIN_VALUE))
        .isEqualTo(OtelTraceState.getMaxP() - 1);
    assertThat(ConsistentSampler.getLowerBoundP(0.0)).isEqualTo(OtelTraceState.getMaxP());
  }

  @Test
  void testGetUpperBoundP() {
    assertThat(ConsistentSampler.getUpperBoundP(1.0)).isEqualTo(0);
    assertThat(ConsistentSampler.getUpperBoundP(Math.nextDown(1.0))).isEqualTo(1);
    for (int i = 1; i < OtelTraceState.getMaxP() - 1; i += 1) {
      double samplingProbability = Math.pow(0.5, i);
      assertThat(ConsistentSampler.getUpperBoundP(samplingProbability)).isEqualTo(i);
      assertThat(ConsistentSampler.getUpperBoundP(Math.nextUp(samplingProbability))).isEqualTo(i);
      assertThat(ConsistentSampler.getUpperBoundP(Math.nextDown(samplingProbability)))
          .isEqualTo(i + 1);
    }
    assertThat(ConsistentSampler.getUpperBoundP(Double.MIN_NORMAL))
        .isEqualTo(OtelTraceState.getMaxP());
    assertThat(ConsistentSampler.getUpperBoundP(Double.MIN_VALUE))
        .isEqualTo(OtelTraceState.getMaxP());
    assertThat(ConsistentSampler.getUpperBoundP(0.0)).isEqualTo(OtelTraceState.getMaxP());
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

  private static ConsistentSampler createConsistentSampler(int p, int r) {
    long randomLong = ~(0xFFFFFFFFFFFFFFFFL << r);
    RandomGenerator randomGenerator = RandomGenerator.create(() -> randomLong);

    return new ConsistentSampler(s -> randomGenerator.numberOfLeadingZerosOfRandomLong()) {
      @Override
      public String getDescription() {
        throw new UnsupportedOperationException();
      }

      @Override
      protected int getP(int parentP, boolean isRoot) {
        return p;
      }
    };
  }

  private static TraceState createTraceState(int p, int r) {
    OtelTraceState state = OtelTraceState.parse("");
    state.setP(p);
    state.setR(r);
    return TraceState.builder().put(OtelTraceState.TRACE_STATE_KEY, state.serialize()).build();
  }

  private static Context createParentContext(
      String traceId, String spanId, int p, int r, boolean sampled) {
    TraceState parentTraceState = createTraceState(p, r);
    TraceFlags traceFlags = sampled ? TraceFlags.getSampled() : TraceFlags.getDefault();
    SpanContext parentSpanContext =
        SpanContext.create(traceId, spanId, traceFlags, parentTraceState);
    Span parentSpan = Span.wrap(parentSpanContext);
    return parentSpan.storeInContext(Context.root());
  }

  private static boolean getSampledFlag(SamplingResult samplingResult) {
    return SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision());
  }

  private static OptionalInt getP(SamplingResult samplingResult, Context parentContext) {
    Span parentSpan = Span.fromContext(parentContext);
    OtelTraceState otelTraceState =
        OtelTraceState.parse(
            samplingResult
                .getUpdatedTraceState(parentSpan.getSpanContext().getTraceState())
                .get(OtelTraceState.TRACE_STATE_KEY));
    return otelTraceState.hasValidP() ? OptionalInt.of(otelTraceState.getP()) : OptionalInt.empty();
  }

  private static OptionalInt getR(SamplingResult samplingResult, Context parentContext) {
    Span parentSpan = Span.fromContext(parentContext);
    OtelTraceState otelTraceState =
        OtelTraceState.parse(
            samplingResult
                .getUpdatedTraceState(parentSpan.getSpanContext().getTraceState())
                .get(OtelTraceState.TRACE_STATE_KEY));
    return otelTraceState.hasValidR() ? OptionalInt.of(otelTraceState.getR()) : OptionalInt.empty();
  }

  private static void assertConsistentSampling(
      int parentP,
      int parentR,
      boolean parentSampled,
      int samplerP,
      int generatedR,
      int expectedP,
      int expectedR,
      boolean expectSampled) {

    String traceId = "0123456789abcdef0123456789abcdef";
    String spanId = "0123456789abcdef";
    String name = "name";
    SpanKind spanKind = SpanKind.SERVER;
    Attributes attributes = Attributes.empty();
    List<LinkData> parentLinks = Collections.emptyList();

    Context parentContext = createParentContext(traceId, spanId, parentP, parentR, parentSampled);
    ConsistentSampler sampler = createConsistentSampler(samplerP, generatedR);
    SamplingResult samplingResult =
        sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);

    assertThat(getSampledFlag(samplingResult)).isEqualTo(expectSampled);
    OptionalInt p = getP(samplingResult, parentContext);
    if (OtelTraceState.isValidP(expectedP)) {
      assertThat(p.getAsInt()).isEqualTo(expectedP);
    } else {
      assertThat(p.isPresent()).isFalse();
    }
    OptionalInt r = getR(samplingResult, parentContext);
    if (OtelTraceState.isValidR(expectedR)) {
      assertThat(r.getAsInt()).isEqualTo(expectedR);
    } else {
      assertThat(r.isPresent()).isFalse();
    }
  }

  private static final boolean NOT_SAMPLED = false;
  private static final boolean SAMPLED = true;

  @Test
  void testUndefinedParentTraceState() {
    assertConsistentSampling(getInvalidP(), getInvalidR(), NOT_SAMPLED, 0, 0, 0, 0, SAMPLED);
    assertConsistentSampling(getInvalidP(), getInvalidR(), NOT_SAMPLED, 2, 3, 2, 3, SAMPLED);
    assertConsistentSampling(
        getInvalidP(), getInvalidR(), NOT_SAMPLED, 3, 2, getInvalidP(), 2, NOT_SAMPLED);
    assertConsistentSampling(getInvalidP(), getInvalidR(), NOT_SAMPLED, 0, 1, 0, 1, SAMPLED);
    assertConsistentSampling(getInvalidP(), getInvalidR(), NOT_SAMPLED, 0, 2, 0, 2, SAMPLED);
    assertConsistentSampling(
        getInvalidP(), getInvalidR(), NOT_SAMPLED, 1, 0, getInvalidP(), 0, NOT_SAMPLED);
    assertConsistentSampling(
        getInvalidP(), getInvalidR(), NOT_SAMPLED, 2, 0, getInvalidP(), 0, NOT_SAMPLED);
    assertConsistentSampling(getInvalidP(), getInvalidR(), SAMPLED, 0, 0, 0, 0, SAMPLED);
    assertConsistentSampling(getInvalidP(), getInvalidR(), SAMPLED, 2, 3, 2, 3, SAMPLED);
    assertConsistentSampling(
        getInvalidP(), getInvalidR(), SAMPLED, 3, 2, getInvalidP(), 2, NOT_SAMPLED);
  }

  @Test
  void testParentTraceStateWithDefinedPOnly() {
    assertConsistentSampling(6, getInvalidR(), NOT_SAMPLED, 0, 0, 0, 0, SAMPLED);
    assertConsistentSampling(7, getInvalidR(), NOT_SAMPLED, 2, 3, 2, 3, SAMPLED);
    assertConsistentSampling(4, getInvalidR(), NOT_SAMPLED, 3, 2, getInvalidP(), 2, NOT_SAMPLED);
    assertConsistentSampling(3, getInvalidR(), NOT_SAMPLED, 0, 1, 0, 1, SAMPLED);
    assertConsistentSampling(2, getInvalidR(), NOT_SAMPLED, 0, 2, 0, 2, SAMPLED);
    assertConsistentSampling(6, getInvalidR(), NOT_SAMPLED, 1, 0, getInvalidP(), 0, NOT_SAMPLED);
    assertConsistentSampling(7, getInvalidR(), NOT_SAMPLED, 2, 0, getInvalidP(), 0, NOT_SAMPLED);
    assertConsistentSampling(5, getInvalidR(), NOT_SAMPLED, 8, 7, getInvalidP(), 7, NOT_SAMPLED);
    assertConsistentSampling(5, getInvalidR(), NOT_SAMPLED, 6, 7, 6, 7, SAMPLED);
    assertConsistentSampling(12, getInvalidR(), SAMPLED, 0, 0, 0, 0, SAMPLED);
    assertConsistentSampling(15, getInvalidR(), SAMPLED, 2, 3, 2, 3, SAMPLED);
    assertConsistentSampling(18, getInvalidR(), SAMPLED, 3, 2, getInvalidP(), 2, NOT_SAMPLED);
  }

  @Test
  void testParentTraceStateWithDefinedROnly() {
    assertConsistentSampling(getInvalidP(), 0, NOT_SAMPLED, 0, 5, 0, 0, SAMPLED);
    assertConsistentSampling(getInvalidP(), 3, NOT_SAMPLED, 2, 0, 2, 3, SAMPLED);
    assertConsistentSampling(getInvalidP(), 2, NOT_SAMPLED, 3, 1, getInvalidP(), 2, NOT_SAMPLED);
    assertConsistentSampling(getInvalidP(), 1, NOT_SAMPLED, 0, 0, 0, 1, SAMPLED);
    assertConsistentSampling(getInvalidP(), 2, NOT_SAMPLED, 0, 5, 0, 2, SAMPLED);
    assertConsistentSampling(getInvalidP(), 0, NOT_SAMPLED, 1, 8, getInvalidP(), 0, NOT_SAMPLED);
    assertConsistentSampling(getInvalidP(), 0, NOT_SAMPLED, 2, 5, getInvalidP(), 0, NOT_SAMPLED);
    assertConsistentSampling(getInvalidP(), 0, SAMPLED, 0, 11, 0, 0, SAMPLED);
    assertConsistentSampling(getInvalidP(), 3, SAMPLED, 2, 9, 2, 3, SAMPLED);
    assertConsistentSampling(getInvalidP(), 2, SAMPLED, 3, 1, getInvalidP(), 2, NOT_SAMPLED);
  }

  @Test
  void testConsistentParentTraceState() {
    // ( (r >= p) <=> sampled) is satisfied
    assertConsistentSampling(3, 5, SAMPLED, 6, 7, getInvalidP(), 5, NOT_SAMPLED);
    assertConsistentSampling(3, 5, SAMPLED, 2, 7, 2, 5, SAMPLED);
    assertConsistentSampling(5, 3, NOT_SAMPLED, 6, 7, getInvalidP(), 3, NOT_SAMPLED);
  }

  @Test
  void testInconsistentParentTraceState() {
    // ( (r >= p) <=> sampled) is not satisfied
    assertConsistentSampling(5, 3, SAMPLED, 6, 7, getInvalidP(), 3, NOT_SAMPLED);
    assertConsistentSampling(3, 5, NOT_SAMPLED, 6, 7, getInvalidP(), 5, NOT_SAMPLED);
    assertConsistentSampling(5, 3, SAMPLED, 1, 7, 1, 3, SAMPLED);
    assertConsistentSampling(3, 5, NOT_SAMPLED, 2, 7, 2, 5, SAMPLED);
  }

  @Test
  void testInvalidSamplerP() {
    assertConsistentSampling(3, 5, SAMPLED, getInvalidP(), 7, getInvalidP(), 5, SAMPLED);
    assertConsistentSampling(5, 3, NOT_SAMPLED, getInvalidP(), 7, getInvalidP(), 3, NOT_SAMPLED);
  }
}
