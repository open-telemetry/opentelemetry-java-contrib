/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.util.RandomGenerator;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsistentRateLimitingSamplerTest {

  private long[] nanoTime;
  private LongSupplier nanoTimeSupplier;
  private Context parentContext;
  private String traceId;
  private String name;
  private SpanKind spanKind;
  private Attributes attributes;
  private List<LinkData> parentLinks;

  @BeforeEach
  void init() {
    nanoTime = new long[] {0L};
    nanoTimeSupplier = () -> nanoTime[0];
    parentContext = Context.root();
    traceId = "0123456789abcdef0123456789abcdef";
    name = "name";
    spanKind = SpanKind.SERVER;
    attributes = Attributes.empty();
    parentLinks = Collections.emptyList();
  }

  private void advanceTime(long nanosIncrement) {
    nanoTime[0] += nanosIncrement;
  }

  private long getCurrentTimeNanos() {
    return nanoTime[0];
  }

  @Test
  void testConstantRate() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;
    SplittableRandom random = new SplittableRandom(0L);

    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            targetSpansPerSecondLimit,
            adaptationTimeSeconds,
            RandomGenerator.create(random::nextLong),
            nanoTimeSupplier);

    long nanosBetweenSpans = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans = 1000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans; ++i) {
      advanceTime(nanosBetweenSpans);
      SamplingResult samplingResult =
          sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }

    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(95) && x <= TimeUnit.SECONDS.toNanos(100))
            .count();

    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }

  @Test
  void testRateIncrease() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;
    SplittableRandom random = new SplittableRandom(0L);

    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            targetSpansPerSecondLimit,
            adaptationTimeSeconds,
            RandomGenerator.create(random::nextLong),
            nanoTimeSupplier);

    long nanosBetweenSpans1 = TimeUnit.MICROSECONDS.toNanos(100);
    long nanosBetweenSpans2 = TimeUnit.MICROSECONDS.toNanos(10);
    int numSpans1 = 500000;
    int numSpans2 = 5000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans1; ++i) {
      advanceTime(nanosBetweenSpans1);
      SamplingResult samplingResult =
          sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }
    for (int i = 0; i < numSpans2; ++i) {
      advanceTime(nanosBetweenSpans2);
      SamplingResult samplingResult =
          sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }

    long numSampledSpansWithin5SecondsBeforeChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(45) && x <= TimeUnit.SECONDS.toNanos(50))
            .count();
    long numSampledSpansWithin5SecondsAfterChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(50) && x <= TimeUnit.SECONDS.toNanos(55))
            .count();
    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(95) && x <= TimeUnit.SECONDS.toNanos(100))
            .count();

    assertThat(numSampledSpansWithin5SecondsBeforeChange / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
    assertThat(numSampledSpansWithin5SecondsAfterChange / 5.)
        .isGreaterThan(2. * targetSpansPerSecondLimit);
    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }

  @Test
  void testRateDecrease() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;
    SplittableRandom random = new SplittableRandom(0L);

    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            targetSpansPerSecondLimit,
            adaptationTimeSeconds,
            RandomGenerator.create(random::nextLong),
            nanoTimeSupplier);

    long nanosBetweenSpans1 = TimeUnit.MICROSECONDS.toNanos(10);
    long nanosBetweenSpans2 = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans1 = 5000000;
    int numSpans2 = 500000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans1; ++i) {
      advanceTime(nanosBetweenSpans1);
      SamplingResult samplingResult =
          sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }
    for (int i = 0; i < numSpans2; ++i) {
      advanceTime(nanosBetweenSpans2);
      SamplingResult samplingResult =
          sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }

    long numSampledSpansWithin5SecondsBeforeChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(45) && x <= TimeUnit.SECONDS.toNanos(50))
            .count();
    long numSampledSpansWithin5SecondsAfterChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(50) && x <= TimeUnit.SECONDS.toNanos(55))
            .count();
    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(95) && x <= TimeUnit.SECONDS.toNanos(100))
            .count();

    assertThat(numSampledSpansWithin5SecondsBeforeChange / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
    assertThat(numSampledSpansWithin5SecondsAfterChange / 5.)
        .isLessThan(0.5 * targetSpansPerSecondLimit);
    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }
}
