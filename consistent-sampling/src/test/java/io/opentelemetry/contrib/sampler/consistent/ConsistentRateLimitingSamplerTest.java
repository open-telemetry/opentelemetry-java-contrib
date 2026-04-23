/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.TestUtil.generateRandomTraceId;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
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
        new CoinFlipSampler(ComposableSampler.alwaysOff(), ComposableSampler.probability(0.8));
    Sampler sampler =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier));

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
        new CoinFlipSampler(ComposableSampler.alwaysOff(), ComposableSampler.probability(0.8));
    Sampler sampler =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                delegate,
                targetSpansPerSecondLimit,
                adaptationTimeSeconds,
                lowResolutionTimeSupplier));

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

    Sampler sampler =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier));

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

    Sampler sampler =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier));

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

  private long randomInterval(long averageSpanRatePerSecond) {
    double uniform = random.nextDouble();
    double intervalInSeconds = -Math.log(uniform) / averageSpanRatePerSecond;
    return (long) (intervalInSeconds * 1e9);
  }

  @Test
  void testProportionalBehavior() {
    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;
    AttributeKey<String> key = AttributeKey.stringKey("category");

    ComposableSampler delegate =
        new CoinFlipSampler(
            new MarkingSampler(ComposableSampler.probability(0.6), key, "A"),
            new MarkingSampler(ComposableSampler.probability(0.4), key, "B"));

    Sampler sampler =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier));

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
  void testUnstableDelegate() {
    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;

    ComposableSampler delegate =
        new CoinFlipSampler(ComposableSampler.alwaysOff(), ComposableSampler.alwaysOn());

    Sampler sampler =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                delegate, targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier));

    long averageRequestRatePerSecond = 10000;
    int numSpans = 1000000;

    List<Long> spanSampledNanos = new ArrayList<>();

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
      }
    }

    long timeNow = nanoTime[0];
    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream().filter(x -> x > timeNow - 5000000000L && x <= timeNow).count();

    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }

  @Test
  void testLegacyCase() {
    // This test makes sure that the issue
    // https://github.com/open-telemetry/opentelemetry-java-contrib/issues/2007
    // is resolved.

    long averageRequestRatePerSecond = 10000;

    ComposableSampler mockRootSampler = new LegacyLikeComposable(ComposableSampler.alwaysOn());

    double targetSpansPerSecondLimit = 2500; // for stage2
    double adaptationTimeSeconds = 5;

    Sampler stage1 =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                mockRootSampler,
                2 * targetSpansPerSecondLimit,
                adaptationTimeSeconds,
                nanoTimeSupplier));

    Sampler stage2 =
        CompositeSampler.wrap(
            ConsistentSampler.rateLimited(
                ComposableSampler.parentThreshold(ComposableSampler.alwaysOff()),
                targetSpansPerSecondLimit,
                adaptationTimeSeconds,
                nanoTimeSupplier));

    int numSpans = 1000000;
    int stage1SampledCount = 0;
    int stage2SampledCount = 0;

    for (int i = 0; i < numSpans; ++i) {
      advanceTime(randomInterval(averageRequestRatePerSecond));
      String traceId = generateRandomTraceId(random);

      SamplingResult samplingResult1 =
          stage1.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);

      boolean isSampled = SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult1.getDecision());
      if (isSampled) {
        stage1SampledCount++;
      }

      Span parentSpan = Span.fromContext(parentContext);
      SpanContext parentSpanContext = parentSpan.getSpanContext();
      TraceState parentSamplingTraceState =
          samplingResult1.getUpdatedTraceState(parentSpanContext.getTraceState());

      SpanContext childSpanContext =
          SpanContext.create(
              traceId,
              "1000badbadbad000",
              isSampled ? TraceFlags.getSampled() : TraceFlags.getDefault(),
              parentSamplingTraceState);
      Span childSpan = Span.wrap(childSpanContext);
      Context childContext = childSpan.storeInContext(parentContext);

      SamplingResult samplingResult2 =
          stage2.shouldSample(childContext, traceId, name, spanKind, attributes, parentLinks);

      if (SamplingDecision.RECORD_AND_SAMPLE.equals(samplingResult2.getDecision())) {
        stage2SampledCount++;
      }
    }

    long timeNow = nanoTime[0];
    double duration = timeNow / 1000000000.0; // in seconds
    assertThat(duration)
        .isCloseTo(numSpans / (double) averageRequestRatePerSecond, Percentage.withPercentage(2));

    assertThat(stage1SampledCount / duration)
        .isCloseTo(2 * targetSpansPerSecondLimit, Percentage.withPercentage(2));

    assertThat(stage2SampledCount / duration)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(2));
  }

  /** Simulates the behavior of a legacy (non consistent-probability) sampler. */
  private static final class LegacyLikeComposable implements ComposableSampler {

    private final ComposableSampler delegate;

    LegacyLikeComposable(ComposableSampler delegate) {
      this.delegate = delegate;
    }

    @Override
    public SamplingIntent getSamplingIntent(
        Context parentContext,
        String traceId,
        String name,
        SpanKind spanKind,
        Attributes attributes,
        List<LinkData> parentLinks) {

      SamplingIntent delegateIntent =
          delegate.getSamplingIntent(
              parentContext, traceId, name, spanKind, attributes, parentLinks);

      // Forcing "legacy" behavior, no threshold will be put into TraceState.
      return SamplingIntent.create(
          delegateIntent.getThreshold(),
          /* thresholdReliable= */ false,
          delegateIntent.getAttributes(),
          delegateIntent.getTraceStateUpdater());
    }

    @Override
    public String getDescription() {
      return "LegacyLike(" + delegate.getDescription() + ")";
    }
  }

  @Test
  void testDescription() {
    double targetSpansPerSecondLimit = 123.456;
    double adaptationTimeSeconds = 7.89;
    ComposableSampler sampler =
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
