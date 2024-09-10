/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.TestUtil.generateRandomTraceId;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
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
  private LongSupplier lowResolutionTimeSupplier;
  private Context parentContext;
  private String name;
  private SpanKind spanKind;
  private Attributes attributes;
  private List<LinkData> parentLinks;
  private SplittableRandom random;

  @BeforeEach
  void init() {
    nanoTime = new long[] {0L};
    nanoTimeSupplier = () -> nanoTime[0];
    lowResolutionTimeSupplier = () -> (nanoTime[0] / 1000000) * 1000000; // 1ms resolution
    parentContext = Context.root();
    name = "name";
    spanKind = SpanKind.SERVER;
    attributes = Attributes.empty();
    parentLinks = Collections.emptyList();
    random = new SplittableRandom(0L);
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

    ComposableSampler delegate =
        new CoinFlipSampler(ConsistentSampler.alwaysOff(), ConsistentSampler.probabilityBased(0.8));
    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);

    long nanosBetweenSpans = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans = 1000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans; ++i) {
      advanceTime(nanosBetweenSpans);
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
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
  void testConstantRateLowResolution() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;

    ComposableSampler delegate =
        new CoinFlipSampler(ConsistentSampler.alwaysOff(), ConsistentSampler.probabilityBased(0.8));
    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, lowResolutionTimeSupplier);

    long nanosBetweenSpans = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans = 1000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans; ++i) {
      advanceTime(nanosBetweenSpans);
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
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

    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);

    long nanosBetweenSpans1 = TimeUnit.MICROSECONDS.toNanos(100);
    long nanosBetweenSpans2 = TimeUnit.MICROSECONDS.toNanos(10);
    int numSpans1 = 500000;
    int numSpans2 = 5000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans1; ++i) {
      advanceTime(nanosBetweenSpans1);
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }
    for (int i = 0; i < numSpans2; ++i) {
      advanceTime(nanosBetweenSpans2);
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
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

    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);

    long nanosBetweenSpans1 = TimeUnit.MICROSECONDS.toNanos(10);
    long nanosBetweenSpans2 = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans1 = 5000000;
    int numSpans2 = 500000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans1; ++i) {
      advanceTime(nanosBetweenSpans1);
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }
    for (int i = 0; i < numSpans2; ++i) {
      advanceTime(nanosBetweenSpans2);
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
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

  /**
   * Generate a random number representing time elapsed between two simulated (root) spans.
   *
   * @param averageSpanRatePerSecond number of simulated spans for each simulated second
   * @return the time in nanos to be used by the simulator
   */
  private long randomInterval(long averageSpanRatePerSecond) {
    // For simulating real traffic, for example as coming from the Internet.
    // Assuming Poisson distribution of incoming requests, averageNumberOfSpanPerSecond
    // is the lambda parameter of the distribution. Consequently, the time between requests
    // has Exponential distribution with the same lambda parameter.
    double uniform = random.nextDouble();
    double intervalInSeconds = -Math.log(uniform) / averageSpanRatePerSecond;
    return (long) (intervalInSeconds * 1e9);
  }

  @Test
  void testProportionalBehavior() {
    // Based on example discussed at https://github.com/open-telemetry/oteps/pull/250
    // Assume that there are 2 categories A and B of spans.
    // Assume there are 10,000 spans/s and 50% belong to A and 50% belong to B.
    // Now we want to sample A with a probability of 60% and B with a probability of 40%.
    // That means we would sample 30,000 spans/s from A and 20,000 spans/s from B.
    //
    // However, if we do not want to sample more than 1000 spans/s overall, our expectation is
    // that the ratio of the sampled A and B spans will still remain 3:2.

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;
    AttributeKey<String> key = AttributeKey.stringKey("category");

    ComposableSampler delegate =
        new CoinFlipSampler(
            new MarkingSampler(ConsistentSampler.probabilityBased(0.6), key, "A"),
            new MarkingSampler(ConsistentSampler.probabilityBased(0.4), key, "B"));

    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(
            delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier);

    long averageRequestRatePerSecond = 10000;
    int numSpans = 1000000;

    List<Long> spanSampledNanos = new ArrayList<>();
    int catAsampledCount = 0;
    int catBsampledCount = 0;

    for (int i = 0; i < numSpans; ++i) {
      advanceTime(randomInterval(averageRequestRatePerSecond));
      SamplingResult samplingResult =
          sampler.shouldSample(
              parentContext,
              generateRandomTraceId(random),
              name,
              spanKind,
              attributes,
              parentLinks);
      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult.getDecision())) {
        spanSampledNanos.add(getCurrentTimeNanos());

        // ConsistentRateLimiting sampler is expected to provide proportional sampling
        // at all times, no need to skip the warm-up phase
        String category = samplingResult.getAttributes().get(key);
        if ("A".equals(category)) {
          catAsampledCount++;
        } else if ("B".equals(category)) {
          catBsampledCount++;
        }
      }
    }

    double expectedRatio = 0.6 / 0.4;
    assertThat(catAsampledCount / (double) catBsampledCount)
        .isCloseTo(expectedRatio, Percentage.withPercentage(2));

    long timeNow = nanoTime[0];
    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream().filter(x -> x > timeNow - 5000000000L && x <= timeNow).count();

    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }

  @Test
  void testDescription() {

    double targetSpansPerSecondLimit = 123.456;
    double adaptationTimeSeconds = 7.89;
    ConsistentSampler sampler =
        ConsistentSampler.rateLimited(targetSpansPerSecondLimit, adaptationTimeSeconds);

    assertThat(sampler.getDescription())
        .isEqualTo(
            "ConsistentRateLimitingSampler{targetSpansPerSecondLimit="
                + targetSpansPerSecondLimit
                + ", adaptationTimeSeconds="
                + adaptationTimeSeconds
                + "}");
  }
}
