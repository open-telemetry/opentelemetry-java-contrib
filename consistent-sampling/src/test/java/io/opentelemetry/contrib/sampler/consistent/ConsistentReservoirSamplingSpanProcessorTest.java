/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentReservoirSamplingSpanProcessor.DEFAULT_EXPORT_TIMEOUT_NANOS;
import static io.opentelemetry.contrib.sampler.consistent.TestUtil.verifyObservedPvaluesUsingGtest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.hipparchus.distribution.discrete.BinomialDistribution;
import org.hipparchus.stat.inference.GTest;
import org.hipparchus.stat.inference.TTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

class ConsistentReservoirSamplingSpanProcessorTest {

  private static final String SPAN_NAME_1 = "MySpanName/1";
  private static final String SPAN_NAME_2 = "MySpanName/2";
  private static final String SPAN_NAME_3 = "MySpanName/3";
  private static final int RESERVOIR_SIZE = 4096;
  private static final long EXPORT_PERIOD_10_MILLIS_AS_NANOS = TimeUnit.MILLISECONDS.toNanos(10);
  private static final long EXPORT_PERIOD_100_MILLIS_AS_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
  private static final long VERY_LONG_EXPORT_PERIOD_NANOS = TimeUnit.SECONDS.toNanos(10000);

  private static void shutdown(SdkTracerProvider sdkTracerProvider) {
    sdkTracerProvider.shutdown().join(1000, TimeUnit.SECONDS);
  }

  private static class WaitingSpanExporter implements SpanExporter {

    private final List<SpanData> spanDataList = new ArrayList<>();
    private final int numberOfSpansToWaitFor;
    private volatile CountDownLatch countDownLatch;
    private final AtomicBoolean shutDownCalled = new AtomicBoolean(false);

    WaitingSpanExporter(int numberOfSpansToWaitFor) {
      countDownLatch = new CountDownLatch(numberOfSpansToWaitFor);
      this.numberOfSpansToWaitFor = numberOfSpansToWaitFor;
    }

    List<SpanData> getExported() {
      List<SpanData> result = new ArrayList<>(spanDataList);
      spanDataList.clear();
      return result;
    }

    /**
     * Waits until {@code numberOfSpansToWaitFor} spans have been exported. Returns the list of
     * exported {@link SpanData} objects, otherwise {@code null} if the current thread is
     * interrupted.
     *
     * @return the list of exported {@link SpanData} objects, otherwise {@code null} if the current
     *     thread is interrupted.
     */
    @Nullable
    List<SpanData> waitForExport() {
      try {
        countDownLatch.await();
      } catch (InterruptedException e) {
        // Preserve the interruption status as per guidance.
        Thread.currentThread().interrupt();
        return null;
      }
      return getExported();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      this.spanDataList.addAll(spans);
      for (int i = 0; i < spans.size(); i++) {
        countDownLatch.countDown();
      }
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
      shutDownCalled.set(true);
      return CompletableResultCode.ofSuccess();
    }

    public void reset() {
      this.countDownLatch = new CountDownLatch(numberOfSpansToWaitFor);
    }
  }

  @Nullable
  private ReadableSpan createEndedSpan(String spanName, SdkTracerProvider sdkTracerProvider) {
    Tracer tracer = sdkTracerProvider.get(getClass().getName());
    Span span = tracer.spanBuilder(spanName).startSpan();
    span.end();
    if (span instanceof ReadableSpan) {
      return (ReadableSpan) span;
    } else {
      return null;
    }
  }

  @Test
  void invalidConfig() {
    SpanExporter exporter = mock(SpanExporter.class);
    when(exporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    assertThatThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(null, 1, 1))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("spanExporter");
    assertThatThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(exporter, -1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reservoir size must be positive");
    assertThatThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(exporter, 1, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("export period must be positive");
    assertThatThrownBy(() -> ConsistentReservoirSamplingSpanProcessor.create(exporter, 1, 1, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("exporter timeout must be positive");
    assertThatThrownBy(
            () -> ConsistentReservoirSamplingSpanProcessor.create(exporter, 1, 1, 1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("randomGenerator");
  }

  @Test
  void startEndRequirements() {
    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            new WaitingSpanExporter(0), RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS);
    assertThat(processor.isStartRequired()).isFalse();
    assertThat(processor.isEndRequired()).isTrue();
  }

  @Test
  @Timeout(10)
  void exportDifferentSampledSpans() {
    WaitingSpanExporter exporter = new WaitingSpanExporter(2);
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingSpanProcessor.create(
                    exporter, RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS))
            .build();

    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1, sdkTracerProvider);
    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2, sdkTracerProvider);
    ReadableSpan span3 = createEndedSpan(SPAN_NAME_3, sdkTracerProvider);
    List<SpanData> exported = exporter.waitForExport();
    assertThat(exported)
        .containsExactlyInAnyOrder(span1.toSpanData(), span2.toSpanData(), span3.toSpanData());

    shutdown(sdkTracerProvider);
  }

  @Test
  @Timeout(10)
  void forceExport() {
    WaitingSpanExporter exporter = new WaitingSpanExporter(100);
    int reservoirSize = 50;
    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            exporter, reservoirSize, VERY_LONG_EXPORT_PERIOD_NANOS);

    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(processor).build();
    for (int i = 0; i < 100; i++) {
      createEndedSpan("MySpanName/" + i, sdkTracerProvider);
    }

    processor.forceFlush().join(10, TimeUnit.SECONDS);
    List<SpanData> exported = exporter.getExported();
    assertThat(exported).isNotNull();
    assertThat(exported.size()).isEqualTo(reservoirSize);

    shutdown(sdkTracerProvider);
  }

  @Test
  @Timeout(10)
  void exportSpansToMultipleServices() {
    WaitingSpanExporter waitingSpanExporter1 = new WaitingSpanExporter(2);
    WaitingSpanExporter waitingSpanExporter2 = new WaitingSpanExporter(2);
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingSpanProcessor.create(
                    SpanExporter.composite(
                        Arrays.asList(waitingSpanExporter1, waitingSpanExporter2)),
                    RESERVOIR_SIZE,
                    EXPORT_PERIOD_100_MILLIS_AS_NANOS))
            .build();

    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1, sdkTracerProvider);
    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2, sdkTracerProvider);
    List<SpanData> exported1 = waitingSpanExporter1.waitForExport();
    List<SpanData> exported2 = waitingSpanExporter2.waitForExport();
    assertThat(exported1).containsExactlyInAnyOrder(span1.toSpanData(), span2.toSpanData());
    assertThat(exported2).containsExactlyInAnyOrder(span1.toSpanData(), span2.toSpanData());

    shutdown(sdkTracerProvider);
  }

  @Test
  void ignoresNullSpans() {
    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            mock(SpanExporter.class), RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS);
    assertThatCode(
            () -> {
              processor.onStart(null, null);
              processor.onEnd(null);
            })
        .doesNotThrowAnyException();

    processor.shutdown();
  }

  @Test
  @Timeout(10)
  void exporterThrowsException() {
    SpanExporter failingExporter = mock(SpanExporter.class);
    when(failingExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    doThrow(new IllegalArgumentException("No export for you."))
        .when(failingExporter)
        .export(ArgumentMatchers.anyList());

    WaitingSpanExporter workingExporter = new WaitingSpanExporter(1);

    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingSpanProcessor.create(
                    SpanExporter.composite(Arrays.asList(failingExporter, workingExporter)),
                    RESERVOIR_SIZE,
                    EXPORT_PERIOD_100_MILLIS_AS_NANOS))
            .build();

    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1, sdkTracerProvider);
    List<SpanData> exported = workingExporter.waitForExport();
    assertThat(exported).containsExactly(span1.toSpanData());

    workingExporter.reset();

    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2, sdkTracerProvider);
    exported = workingExporter.waitForExport();
    assertThat(exported).containsExactly(span2.toSpanData());

    shutdown(sdkTracerProvider);
  }

  private static ArgumentMatcher<Collection<SpanData>> containsSpanName(
      String spanName, Runnable runOnMatch) {
    return spans -> {
      assertThat(spans).anySatisfy(span -> assertThat(span.getName()).isEqualTo(spanName));
      runOnMatch.run();
      return true;
    };
  }

  private static void awaitReservoirEmpty(SpanProcessor processor) {
    await()
        .untilAsserted(
            () ->
                assertThat(
                        ((ConsistentReservoirSamplingSpanProcessor) processor).isReservoirEmpty())
                    .isTrue());
  }

  @Test
  @Timeout(10)
  public void continuesIfExporterTimesOut() throws InterruptedException {
    SpanExporter mockSpanExporter = mock(SpanExporter.class);
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            mockSpanExporter,
            RESERVOIR_SIZE,
            EXPORT_PERIOD_10_MILLIS_AS_NANOS,
            TimeUnit.MILLISECONDS.toNanos(1));
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(processor).build();

    CountDownLatch exported = new CountDownLatch(1);

    // We return a result we never complete, meaning it will timeout.
    when(mockSpanExporter.export(argThat(containsSpanName(SPAN_NAME_1, exported::countDown))))
        .thenReturn(new CompletableResultCode());

    createEndedSpan(SPAN_NAME_1, sdkTracerProvider);
    exported.await();

    // Timed out so the span was dropped.
    awaitReservoirEmpty(processor);

    // Still processing new spans.
    CountDownLatch exportedAgain = new CountDownLatch(1);
    reset(mockSpanExporter);
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    when(mockSpanExporter.export(argThat(containsSpanName(SPAN_NAME_2, exportedAgain::countDown))))
        .thenReturn(CompletableResultCode.ofSuccess());
    createEndedSpan(SPAN_NAME_2, sdkTracerProvider);
    exported.await();
    awaitReservoirEmpty(processor);

    shutdown(sdkTracerProvider);
  }

  @Test
  @Timeout(10)
  void exportNotSampledNotRecordedSpans() {
    Sampler mockSampler = mock(Sampler.class);
    WaitingSpanExporter exporter = new WaitingSpanExporter(1);
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingSpanProcessor.create(
                    exporter, RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS))
            .setSampler(mockSampler)
            .build();

    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.drop());
    sdkTracerProvider.get("test").spanBuilder(SPAN_NAME_1).startSpan().end();
    sdkTracerProvider.get("test").spanBuilder(SPAN_NAME_2).startSpan().end();
    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.recordAndSample());
    ReadableSpan span = createEndedSpan(SPAN_NAME_2, sdkTracerProvider);
    // Spans are recorded and exported in the same order as they are ended, we test that a non
    // sampled span is not exported by creating and ending a sampled span after a non sampled span
    // and checking that the first exported span is the sampled span (the non sampled did not get
    // exported).
    List<SpanData> exported = exporter.waitForExport();
    assertThat(exported).containsExactly(span.toSpanData());

    shutdown(sdkTracerProvider);
  }

  @Test
  @Timeout(10)
  void exportNotSampledButRecordedSpans() {
    WaitingSpanExporter exporter = new WaitingSpanExporter(1);

    Sampler mockSampler = mock(Sampler.class);
    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.recordOnly());
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingSpanProcessor.create(
                    exporter, RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS))
            .setSampler(mockSampler)
            .build();

    createEndedSpan(SPAN_NAME_1, sdkTracerProvider);
    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.recordAndSample());
    ReadableSpan span = createEndedSpan(SPAN_NAME_2, sdkTracerProvider);

    // Spans are recorded and exported in the same order as they are ended, we test that a non
    // exported span is not exported by creating and ending a sampled span after a non sampled span
    // and checking that the first exported span is the sampled span (the non sampled did not get
    // exported).
    List<SpanData> exported = exporter.waitForExport();
    assertThat(exported).containsExactly(span.toSpanData());

    shutdown(sdkTracerProvider);
  }

  @Test
  @Timeout(10)
  void shutdownFlushes() {
    WaitingSpanExporter exporter = new WaitingSpanExporter(1);

    // Set the export period to large value, in order to confirm the #flush() below works
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingSpanProcessor.create(
                    exporter, RESERVOIR_SIZE, VERY_LONG_EXPORT_PERIOD_NANOS))
            .build();

    ReadableSpan span = createEndedSpan(SPAN_NAME_1, sdkTracerProvider);

    // Force a shutdown, which forces processing of all remaining spans.
    shutdown(sdkTracerProvider);

    List<SpanData> exported = exporter.getExported();
    assertThat(exported).containsExactly(span.toSpanData());
    assertThat(exporter.shutDownCalled.get()).isTrue();
  }

  @Test
  void shutdownPropagatesSuccess() {
    SpanExporter mockSpanExporter = mock(SpanExporter.class);
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            mockSpanExporter, RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS);
    CompletableResultCode result = processor.shutdown();
    result.join(1, TimeUnit.SECONDS);
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shutdownPropagatesFailure() {
    SpanExporter mockSpanExporter = mock(SpanExporter.class);
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofFailure());

    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            mockSpanExporter, RESERVOIR_SIZE, EXPORT_PERIOD_100_MILLIS_AS_NANOS);
    CompletableResultCode result = processor.shutdown();
    result.join(1, TimeUnit.SECONDS);
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  @Timeout(10)
  void fullReservoir() {
    int reservoirSize = 10;
    int numberOfSpans = 100;

    WaitingSpanExporter exporter = new WaitingSpanExporter(reservoirSize);

    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            exporter, reservoirSize, VERY_LONG_EXPORT_PERIOD_NANOS);

    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .setSampler(ConsistentSampler.alwaysOn())
            .addSpanProcessor(processor)
            .build();

    IntStream.range(0, numberOfSpans)
        .forEach(i -> createEndedSpan("MySpanName/" + i, sdkTracerProvider));

    processor.forceFlush().join(10, TimeUnit.SECONDS);

    List<SpanData> exported = exporter.waitForExport();
    assertThat(exported).hasSize(reservoirSize);

    shutdown(sdkTracerProvider);
  }

  private enum Tests {
    VERIFY_MEAN,
    VERIFY_PVALUE_DISTRIBUTION,
    VERIFY_ORDER_INDEPENDENCE
  }

  private static LongSupplier asThreadSafeLongSupplier(SplittableRandom rng) {
    return () -> {
      synchronized (rng) {
        return rng.nextLong();
      }
    };
  }

  /**
   * Tests a multi-stage consistent sampling setup consisting of a consistent probability-based
   * sampler with predefined sampling probability followed by a reservoir sampling span processor
   * with fixed reservoir size.
   */
  private void testConsistentSampling(
      long seed,
      int numCycles,
      int numberOfSpans,
      int reservoirSize,
      double samplingProbability,
      EnumSet<Tests> tests) {

    SplittableRandom rng1 = new SplittableRandom(seed);
    SplittableRandom rng2 = rng1.split();

    WaitingSpanExporter spanExporter = new WaitingSpanExporter(0);

    SpanProcessor processor =
        ConsistentReservoirSamplingSpanProcessor.create(
            spanExporter,
            reservoirSize,
            VERY_LONG_EXPORT_PERIOD_NANOS,
            DEFAULT_EXPORT_TIMEOUT_NANOS,
            RandomGenerator.create(asThreadSafeLongSupplier(rng1)));

    RandomGenerator randomGenerator = RandomGenerator.create(asThreadSafeLongSupplier(rng2));
    SdkTracerProvider sdkTracerProvider =
        SdkTracerProvider.builder()
            .setSampler(
                ConsistentSampler.probabilityBased(
                    samplingProbability, s -> randomGenerator.numberOfLeadingZerosOfRandomLong()))
            .addSpanProcessor(processor)
            .build();

    Map<Integer, Long> observedPvalues = new HashMap<>();
    Map<String, Long> spanNameCounts = new HashMap<>();

    double[] totalAdjustedCounts = new double[numCycles];

    for (int k = 0; k < numCycles; ++k) {
      List<ReadableSpan> spans = new ArrayList<>(numberOfSpans);
      for (long i = 0; i < numberOfSpans; ++i) {
        ReadableSpan span = createEndedSpan(Long.toString(i), sdkTracerProvider);
        if (span != null) {
          spans.add(span);
        }
      }

      if (samplingProbability >= 1.) {
        assertThat(spans).hasSize(numberOfSpans);
      }

      processor.forceFlush().join(1000, TimeUnit.SECONDS);

      List<SpanData> exported = spanExporter.getExported();
      assertThat(exported).hasSize(Math.min(reservoirSize, spans.size()));

      long totalAdjustedCount = 0;
      for (SpanData spanData : exported) {
        String traceStateString =
            spanData.getSpanContext().getTraceState().get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState traceState = OtelTraceState.parse(traceStateString);
        assertTrue(traceState.hasValidR());
        assertTrue(traceState.hasValidP());
        observedPvalues.merge(traceState.getP(), 1L, Long::sum);
        totalAdjustedCount += 1L << traceState.getP();
        spanNameCounts.merge(spanData.getName(), 1L, Long::sum);
      }
      totalAdjustedCounts[k] = totalAdjustedCount;
    }

    long totalNumberOfSpans = numberOfSpans * (long) numCycles;
    if (numCycles == 1) {
      assertThat(observedPvalues).hasSizeLessThanOrEqualTo(2);
    }
    if (tests.contains(Tests.VERIFY_MEAN)) {
      assertThat(reservoirSize)
          .isGreaterThanOrEqualTo(
              100); // require a lower limit on the reservoir size, to justify the assumption of the
      // t-test that values are normally distributed

      assertThat(new TTest().tTest(totalNumberOfSpans / (double) numCycles, totalAdjustedCounts))
          .isGreaterThan(0.01);
    }
    if (tests.contains(Tests.VERIFY_PVALUE_DISTRIBUTION)) {
      assertThat(observedPvalues)
          .hasSizeLessThanOrEqualTo(2); // test does not work for more than 2 different p-values

      // The expected number of sampled spans is binomially distributed with the given sampling
      // probability. However, due to the reservoir sampling buffer the maximum number of sampled
      // spans is given by the reservoir size. The effective sampling rate is therefore given by
      // sum_{i=0}^n p^i*(1-p)^{n-i}*min(i,k) (n choose i)
      // where p denotes the sampling rate, n is the total number of original spans, and k denotes
      // the reservoir size
      double p1 =
          new BinomialDistribution(numberOfSpans - 1, samplingProbability)
              .cumulativeProbability(reservoirSize - 1);
      double p2 =
          new BinomialDistribution(numberOfSpans, samplingProbability)
              .cumulativeProbability(reservoirSize);
      assertThat(p1).isLessThanOrEqualTo(p2);

      double effectiveSamplingProbability =
          samplingProbability * p1 + (reservoirSize / (double) numberOfSpans) * (1. - p2);
      verifyObservedPvaluesUsingGtest(
          totalNumberOfSpans, observedPvalues, effectiveSamplingProbability);
    }
    if (tests.contains(Tests.VERIFY_ORDER_INDEPENDENCE)) {
      assertThat(spanNameCounts.size()).isEqualTo(numberOfSpans);
      long[] observed = spanNameCounts.values().stream().mapToLong(x -> x).toArray();
      double[] expected = new double[numberOfSpans];
      Arrays.fill(expected, 1.);
      assertThat(new GTest().gTest(expected, observed)).isGreaterThan(0.01);
    }

    shutdown(sdkTracerProvider);
  }

  @Test
  @Timeout(1000)
  void testConsistentSampling() {
    testConsistentSampling(
        0x34e7052af91d5355L,
        1000,
        1000,
        100,
        1.,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0x44ec62de12a422b4L,
        1000,
        1000,
        100,
        0.8,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0x2c3d086534e14407L,
        1000,
        1000,
        100,
        0.1,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0xd3f8a40433cf0522L,
        1000,
        1000,
        200,
        0.9,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0xf25638ca67eceadcL, 10000, 100, 100, 1.0, EnumSet.of(Tests.VERIFY_MEAN));
    testConsistentSampling(
        0x14c5f8f815618ce2L,
        1000,
        200,
        100,
        1.0,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0xb6c27f1169e128ddL,
        1000,
        1000,
        200,
        0.2,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0xab558ff7c5c73c18L,
        1000,
        10000,
        200,
        1.,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0xe53010c4b009a6c0L,
        1000,
        1000,
        2000,
        0.2,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        0xc41d327fd1a6866aL, 1000000, 5, 4, 1.0, EnumSet.of(Tests.VERIFY_ORDER_INDEPENDENCE));
  }
}
