/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.export;

import static io.opentelemetry.contrib.util.TestUtil.verifyObservedPvaluesUsingGtest;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.internal.GuardedBy;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.samplers.ConsistentSampler;
import io.opentelemetry.contrib.state.OtelTraceState;
import io.opentelemetry.contrib.util.RandomGenerator;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
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
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.hipparchus.distribution.discrete.BinomialDistribution;
import org.hipparchus.stat.descriptive.StatisticalSummary;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.stat.inference.GTest;
import org.hipparchus.stat.inference.TTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@SuppressWarnings("PreferJavaTimeOverload")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsistentReservoirSamplingBatchSpanProcessorTest {

  private static final String SPAN_NAME_1 = "MySpanName/1";
  private static final String SPAN_NAME_2 = "MySpanName/2";
  private static final long MAX_SCHEDULE_DELAY_MILLIS = 500;

  @Nullable private SdkTracerProvider sdkTracerProvider;
  private final BlockingSpanExporter blockingSpanExporter = new BlockingSpanExporter();

  @Mock private Sampler mockSampler;
  @Mock private SpanExporter mockSpanExporter;

  @BeforeEach
  void setUp() {
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
  }

  @AfterEach
  void cleanup() {
    if (sdkTracerProvider != null) {
      sdkTracerProvider.shutdown();
    }
  }

  @Nullable
  private ReadableSpan createEndedSpan(String spanName) {
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
  void configTest_EmptyOptions() {
    ConsistentReservoirSamplingBatchSpanProcessorBuilder config =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(
            new WaitingSpanExporter(0, CompletableResultCode.ofSuccess()));
    Assertions.assertThat(config.getScheduleDelayNanos())
        .isEqualTo(
            TimeUnit.MILLISECONDS.toNanos(
                ConsistentReservoirSamplingBatchSpanProcessorBuilder
                    .DEFAULT_SCHEDULE_DELAY_MILLIS));
    Assertions.assertThat(config.getReservoirSize())
        .isEqualTo(ConsistentReservoirSamplingBatchSpanProcessorBuilder.DEFAULT_RESERVOIR_SIZE);
    Assertions.assertThat(config.getExporterTimeoutNanos())
        .isEqualTo(
            TimeUnit.MILLISECONDS.toNanos(
                ConsistentReservoirSamplingBatchSpanProcessorBuilder
                    .DEFAULT_EXPORT_TIMEOUT_MILLIS));
  }

  @Test
  void invalidConfig() {
    assertThatThrownBy(
            () ->
                ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
                    .setScheduleDelay(-1, TimeUnit.MILLISECONDS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("delay must be non-negative");
    assertThatThrownBy(
            () ->
                ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
                    .setScheduleDelay(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("unit");
    assertThatThrownBy(
            () ->
                ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
                    .setScheduleDelay(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("delay");
    assertThatThrownBy(
            () ->
                ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
                    .setExporterTimeout(-1, TimeUnit.MILLISECONDS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout must be non-negative");
    assertThatThrownBy(
            () ->
                ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
                    .setExporterTimeout(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("unit");
    assertThatThrownBy(
            () ->
                ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
                    .setExporterTimeout(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("timeout");
  }

  @Test
  void startEndRequirements() {
    ConsistentReservoirSamplingBatchSpanProcessor spansProcessor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(
                new WaitingSpanExporter(0, CompletableResultCode.ofSuccess()))
            .build();
    Assertions.assertThat(spansProcessor.isStartRequired()).isFalse();
    Assertions.assertThat(spansProcessor.isEndRequired()).isTrue();
  }

  @Test
  void exportDifferentSampledSpans() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(2, CompletableResultCode.ofSuccess());
    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(waitingSpanExporter)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2);
    List<SpanData> exported = waitingSpanExporter.waitForExport();
    Assertions.assertThat(exported)
        .containsExactlyInAnyOrder(span1.toSpanData(), span2.toSpanData());
  }

  @Test
  void exportMoreSpansThanTheBufferSize() {
    CompletableSpanExporter spanExporter = new CompletableSpanExporter();

    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(spanExporter)
                    .setReservoirSize(6)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span2 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span3 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span4 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span5 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span6 = createEndedSpan(SPAN_NAME_1);

    spanExporter.succeed();

    await()
        .untilAsserted(
            () ->
                Assertions.assertThat(spanExporter.getExported())
                    .containsExactlyInAnyOrder(
                        span1.toSpanData(),
                        span2.toSpanData(),
                        span3.toSpanData(),
                        span4.toSpanData(),
                        span5.toSpanData(),
                        span6.toSpanData()));
  }

  @Test
  void forceExport() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(100, CompletableResultCode.ofSuccess(), 1);
    ConsistentReservoirSamplingBatchSpanProcessor batchSpanProcessor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(waitingSpanExporter)
            // .setReservoirSize(10_000)
            // Force flush should send all spans, make sure the number of spans we check here is
            // not divisible by the batch size.
            .setReservoirSize(49)
            .setScheduleDelay(10, TimeUnit.SECONDS)
            .build();

    sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor(batchSpanProcessor).build();
    for (int i = 0; i < 100; i++) {
      createEndedSpan("notExported");
    }

    batchSpanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
    List<SpanData> exported = waitingSpanExporter.getExported();
    Assertions.assertThat(exported).isNotNull();
    Assertions.assertThat(exported.size()).isEqualTo(49);
  }

  @Test
  void exportSpansToMultipleServices() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(2, CompletableResultCode.ofSuccess());
    WaitingSpanExporter waitingSpanExporter2 =
        new WaitingSpanExporter(2, CompletableResultCode.ofSuccess());
    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(
                        SpanExporter.composite(
                            Arrays.asList(waitingSpanExporter, waitingSpanExporter2)))
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1);
    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2);
    List<SpanData> exported1 = waitingSpanExporter.waitForExport();
    List<SpanData> exported2 = waitingSpanExporter2.waitForExport();
    Assertions.assertThat(exported1)
        .containsExactlyInAnyOrder(span1.toSpanData(), span2.toSpanData());
    Assertions.assertThat(exported2)
        .containsExactlyInAnyOrder(span1.toSpanData(), span2.toSpanData());
  }

  @Test
  void exportMoreSpansThanTheMaximumLimit() {
    int maxQueuedSpans = 8;
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(maxQueuedSpans, CompletableResultCode.ofSuccess());
    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(
                        SpanExporter.composite(
                            Arrays.asList(blockingSpanExporter, waitingSpanExporter)))
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .setReservoirSize(maxQueuedSpans)
                    .build())
            .build();

    List<SpanData> spansToExport = new ArrayList<>(maxQueuedSpans + 1);
    // Wait to block the worker thread in the BatchSampledSpansProcessor. This ensures that no items
    // can be removed from the queue. Need to add a span to trigger the export otherwise the
    // pipeline is never called.
    spansToExport.add(createEndedSpan("blocking_span").toSpanData());
    blockingSpanExporter.waitUntilIsBlocked();

    for (int i = 0; i < maxQueuedSpans; i++) {
      // First export maxQueuedSpans, the worker thread is blocked so all items should be queued.
      spansToExport.add(createEndedSpan("span_1_" + i).toSpanData());
    }

    // TODO: assertThat(spanExporter.getReferencedSpans()).isEqualTo(maxQueuedSpans);

    // Now we should start dropping.
    for (int i = 0; i < 7; i++) {
      createEndedSpan("span_2_" + i);
      // TODO: assertThat(getDroppedSpans()).isEqualTo(i + 1);
    }

    // TODO: assertThat(getReferencedSpans()).isEqualTo(maxQueuedSpans);

    // Release the blocking exporter
    blockingSpanExporter.unblock();

    // While we wait for maxQueuedSpans we ensure that the queue is also empty after this.
    List<SpanData> exported = waitingSpanExporter.waitForExport();
    Assertions.assertThat(exported).isNotNull();
    Assertions.assertThat(exported).hasSize(maxQueuedSpans + 1);
    // assertThat(exported).containsExactlyInAnyOrderElementsOf(spansToExport);
    exported.clear();
    spansToExport.clear();

    waitingSpanExporter.reset();
    // We cannot compare with maxReferencedSpans here because the worker thread may get
    // unscheduled immediately after exporting, but before updating the pushed spans, if that is
    // the case at most bufferSize spans will miss.
    // TODO: assertThat(getPushedSpans()).isAtLeast((long) maxQueuedSpans - maxBatchSize);

    for (int i = 0; i < maxQueuedSpans; i++) {
      spansToExport.add(createEndedSpan("span_3_" + i).toSpanData());
      // No more dropped spans.
      // TODO: assertThat(getDroppedSpans()).isEqualTo(7);
    }

    exported = waitingSpanExporter.waitForExport();
    Assertions.assertThat(exported).isNotNull();
    Assertions.assertThat(exported).containsExactlyInAnyOrderElementsOf(spansToExport);
  }

  @Test
  void ignoresNullSpans() {
    ConsistentReservoirSamplingBatchSpanProcessor processor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter).build();
    try {
      assertThatCode(
              () -> {
                processor.onStart(null, null);
                processor.onEnd(null);
              })
          .doesNotThrowAnyException();
    } finally {
      processor.shutdown();
    }
  }

  @Test
  void exporterThrowsException() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(1, CompletableResultCode.ofSuccess());
    doThrow(new IllegalArgumentException("No export for you."))
        .when(mockSpanExporter)
        .export(ArgumentMatchers.anyList());
    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(
                        SpanExporter.composite(
                            Arrays.asList(mockSpanExporter, waitingSpanExporter)))
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();
    ReadableSpan span1 = createEndedSpan(SPAN_NAME_1);
    List<SpanData> exported = waitingSpanExporter.waitForExport();
    Assertions.assertThat(exported).containsExactly(span1.toSpanData());
    waitingSpanExporter.reset();
    // Continue to export after the exception was received.
    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2);
    exported = waitingSpanExporter.waitForExport();
    Assertions.assertThat(exported).containsExactly(span2.toSpanData());
  }

  @Test
  @Timeout(5)
  public void continuesIfExporterTimesOut() throws InterruptedException {
    int exporterTimeoutMillis = 10;
    ConsistentReservoirSamplingBatchSpanProcessor bsp =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter)
            .setExporterTimeout(exporterTimeoutMillis, TimeUnit.MILLISECONDS)
            .setScheduleDelay(1, TimeUnit.MILLISECONDS)
            .setReservoirSize(1)
            .build();
    sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor(bsp).build();

    CountDownLatch exported = new CountDownLatch(1);
    // We return a result we never complete, meaning it will timeout.
    when(mockSpanExporter.export(
            argThat(
                spans -> {
                  Assertions.assertThat(spans)
                      .anySatisfy(
                          span -> Assertions.assertThat(span.getName()).isEqualTo(SPAN_NAME_1));
                  exported.countDown();
                  return true;
                })))
        .thenReturn(new CompletableResultCode());
    createEndedSpan(SPAN_NAME_1);
    exported.await();
    // Timed out so the span was dropped.
    await().untilAsserted(() -> Assertions.assertThat(bsp.isReservoirEmpty()).isTrue());

    // Still processing new spans.
    CountDownLatch exportedAgain = new CountDownLatch(1);
    reset(mockSpanExporter);
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    when(mockSpanExporter.export(
            argThat(
                spans -> {
                  Assertions.assertThat(spans)
                      .anySatisfy(
                          span -> Assertions.assertThat(span.getName()).isEqualTo(SPAN_NAME_2));
                  exportedAgain.countDown();
                  return true;
                })))
        .thenReturn(CompletableResultCode.ofSuccess());
    createEndedSpan(SPAN_NAME_2);
    exported.await();
    await().untilAsserted(() -> Assertions.assertThat(bsp.isReservoirEmpty()).isTrue());
  }

  @Test
  void exportNotSampledSpans() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(1, CompletableResultCode.ofSuccess());
    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(waitingSpanExporter)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .setSampler(mockSampler)
            .build();

    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.drop());
    sdkTracerProvider.get("test").spanBuilder(SPAN_NAME_1).startSpan().end();
    sdkTracerProvider.get("test").spanBuilder(SPAN_NAME_2).startSpan().end();
    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.recordAndSample());
    ReadableSpan span = createEndedSpan(SPAN_NAME_2);
    // Spans are recorded and exported in the same order as they are ended, we test that a non
    // sampled span is not exported by creating and ending a sampled span after a non sampled span
    // and checking that the first exported span is the sampled span (the non sampled did not get
    // exported).
    List<SpanData> exported = waitingSpanExporter.waitForExport();
    // Need to check this because otherwise the variable span1 is unused, other option is to not
    // have a span1 variable.
    Assertions.assertThat(exported).containsExactly(span.toSpanData());
  }

  @Test
  void exportNotSampledSpans_recordOnly() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(1, CompletableResultCode.ofSuccess());

    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.recordOnly());
    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(waitingSpanExporter)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .setSampler(mockSampler)
            .build();

    createEndedSpan(SPAN_NAME_1);
    when(mockSampler.shouldSample(any(), any(), any(), any(), any(), anyList()))
        .thenReturn(SamplingResult.recordAndSample());
    ReadableSpan span = createEndedSpan(SPAN_NAME_2);

    // Spans are recorded and exported in the same order as they are ended, we test that a non
    // exported span is not exported by creating and ending a sampled span after a non sampled span
    // and checking that the first exported span is the sampled span (the non sampled did not get
    // exported).
    List<SpanData> exported = waitingSpanExporter.waitForExport();
    // Need to check this because otherwise the variable span1 is unused, other option is to not
    // have a span1 variable.
    Assertions.assertThat(exported).containsExactly(span.toSpanData());
  }

  @Test
  @Timeout(10)
  void shutdownFlushes() {
    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(1, CompletableResultCode.ofSuccess());
    // Set the export delay to large value, in order to confirm the #flush() below works

    sdkTracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                ConsistentReservoirSamplingBatchSpanProcessor.builder(waitingSpanExporter)
                    .setScheduleDelay(10, TimeUnit.SECONDS)
                    .build())
            .build();

    ReadableSpan span2 = createEndedSpan(SPAN_NAME_2);

    // Force a shutdown, which forces processing of all remaining spans.
    sdkTracerProvider.shutdown().join(10, TimeUnit.SECONDS);

    List<SpanData> exported = waitingSpanExporter.getExported();
    Assertions.assertThat(exported).containsExactly(span2.toSpanData());
    Assertions.assertThat(waitingSpanExporter.shutDownCalled.get()).isTrue();
  }

  @Test
  void shutdownPropagatesSuccess() {
    ConsistentReservoirSamplingBatchSpanProcessor processor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter).build();
    CompletableResultCode result = processor.shutdown();
    result.join(1, TimeUnit.SECONDS);
    Assertions.assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shutdownPropagatesFailure() {
    when(mockSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofFailure());
    ConsistentReservoirSamplingBatchSpanProcessor processor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(mockSpanExporter).build();
    CompletableResultCode result = processor.shutdown();
    result.join(1, TimeUnit.SECONDS);
    Assertions.assertThat(result.isSuccess()).isFalse();
  }

  private static final class BlockingSpanExporter implements SpanExporter {

    final Object monitor = new Object();

    private enum State {
      WAIT_TO_BLOCK,
      BLOCKED,
      UNBLOCKED
    }

    @GuardedBy("monitor")
    State state = State.WAIT_TO_BLOCK;

    @Override
    public CompletableResultCode export(Collection<SpanData> spanDataList) {
      synchronized (monitor) {
        while (state != State.UNBLOCKED) {
          try {
            state = State.BLOCKED;
            // Some threads may wait for Blocked State.
            monitor.notifyAll();
            monitor.wait();
          } catch (InterruptedException e) {
            // Do nothing
          }
        }
      }
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    private void waitUntilIsBlocked() {
      synchronized (monitor) {
        while (state != State.BLOCKED) {
          try {
            monitor.wait();
          } catch (InterruptedException e) {
            // Do nothing
          }
        }
      }
    }

    @Override
    public CompletableResultCode shutdown() {
      // Do nothing;
      return CompletableResultCode.ofSuccess();
    }

    private void unblock() {
      synchronized (monitor) {
        state = State.UNBLOCKED;
        monitor.notifyAll();
      }
    }
  }

  private static class CompletableSpanExporter implements SpanExporter {

    private final List<CompletableResultCode> results = new ArrayList<>();

    private final List<SpanData> exported = new ArrayList<>();

    private volatile boolean succeeded;

    List<SpanData> getExported() {
      return exported;
    }

    void succeed() {
      succeeded = true;
      results.forEach(CompletableResultCode::succeed);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      exported.addAll(spans);
      if (succeeded) {
        return CompletableResultCode.ofSuccess();
      }
      CompletableResultCode result = new CompletableResultCode();
      results.add(result);
      return result;
    }

    @Override
    public CompletableResultCode flush() {
      if (succeeded) {
        return CompletableResultCode.ofSuccess();
      } else {
        return CompletableResultCode.ofFailure();
      }
    }

    @Override
    public CompletableResultCode shutdown() {
      return flush();
    }
  }

  static class WaitingSpanExporter implements SpanExporter {

    private final List<SpanData> spanDataList = new ArrayList<>();
    private final int numberToWaitFor;
    private final CompletableResultCode exportResultCode;
    private CountDownLatch countDownLatch;
    private int timeout = 10;
    private final AtomicBoolean shutDownCalled = new AtomicBoolean(false);

    WaitingSpanExporter(int numberToWaitFor, CompletableResultCode exportResultCode) {
      countDownLatch = new CountDownLatch(numberToWaitFor);
      this.numberToWaitFor = numberToWaitFor;
      this.exportResultCode = exportResultCode;
    }

    WaitingSpanExporter(int numberToWaitFor, CompletableResultCode exportResultCode, int timeout) {
      this(numberToWaitFor, exportResultCode);
      this.timeout = timeout;
    }

    List<SpanData> getExported() {
      List<SpanData> result = new ArrayList<>(spanDataList);
      spanDataList.clear();
      return result;
    }

    /**
     * Waits until we received numberOfSpans spans to export. Returns the list of exported {@link
     * SpanData} objects, otherwise {@code null} if the current thread is interrupted.
     *
     * @return the list of exported {@link SpanData} objects, otherwise {@code null} if the current
     *     thread is interrupted.
     */
    @Nullable
    List<SpanData> waitForExport() {
      try {
        countDownLatch.await(timeout, TimeUnit.SECONDS);
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
      return exportResultCode;
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
      this.countDownLatch = new CountDownLatch(numberToWaitFor);
    }
  }

  @Test
  void exportDifferentConsistentlySampledSpans() {
    int reservoirSize = 10;
    int numberOfSpans = 100;

    WaitingSpanExporter waitingSpanExporter =
        new WaitingSpanExporter(reservoirSize, CompletableResultCode.ofSuccess());

    ConsistentReservoirSamplingBatchSpanProcessor spanProcessor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(waitingSpanExporter)
            .setReservoirSize(reservoirSize)
            .setScheduleDelay(10, TimeUnit.SECONDS)
            .setReservoirSize(reservoirSize)
            .build();

    sdkTracerProvider =
        SdkTracerProvider.builder()
            .setSampler(ConsistentSampler.alwaysOn())
            .addSpanProcessor(spanProcessor)
            .build();

    List<ReadableSpan> spans =
        IntStream.range(0, numberOfSpans)
            .mapToObj(i -> createEndedSpan("MySpanName/" + i))
            .collect(toList());

    Assertions.assertThat(spans).hasSize(numberOfSpans);

    spanProcessor.forceFlush().join(10, TimeUnit.SECONDS);

    List<SpanData> exported = waitingSpanExporter.waitForExport();
    Assertions.assertThat(exported).hasSize(reservoirSize);
  }

  private enum Tests {
    VERIFY_MEAN,
    VERIFY_PVALUE_DISTRIBUTION,
    VERIFY_ORDER_INDEPENDENCE
  }

  private void testConsistentSampling(
      boolean useAlternativeReservoirImplementation,
      long seed,
      int numCycles,
      int numberOfSpans,
      int reservoirSize,
      double samplingProbability,
      EnumSet<Tests> tests) {

    SplittableRandom rng1 = new SplittableRandom(seed);
    SplittableRandom rng2 = rng1.split();
    RandomGenerator threadSafeRandomGenerator1 =
        () -> {
          synchronized (rng1) {
            return rng1.nextLong();
          }
        };
    RandomGenerator threadSafeRandomGenerator2 =
        () -> {
          synchronized (rng2) {
            return rng1.nextLong();
          }
        };

    WaitingSpanExporter spanExporter =
        new WaitingSpanExporter(0, CompletableResultCode.ofSuccess());

    ConsistentReservoirSamplingBatchSpanProcessor spanProcessor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(spanExporter)
            .setReservoirSize(reservoirSize)
            .setThreadSafeRandomGenerator(threadSafeRandomGenerator1)
            .setScheduleDelay(1000, TimeUnit.SECONDS)
            .setReservoirSize(reservoirSize)
            .useAlternativeReservoirImplementation(useAlternativeReservoirImplementation)
            .build();

    sdkTracerProvider =
        SdkTracerProvider.builder()
            .setSampler(
                ConsistentSampler.probabilityBased(samplingProbability, threadSafeRandomGenerator2))
            .addSpanProcessor(spanProcessor)
            .build();

    Map<Integer, Long> observedPvalues = new HashMap<>();
    Map<String, Long> spanNameCounts = new HashMap<>();

    double[] totalAdjustedCounts = new double[numCycles];

    for (int k = 0; k < numCycles; ++k) {
      String prefixSpanName = "MySpanName/" + k + "/";
      List<ReadableSpan> spans =
          LongStream.range(0, numberOfSpans)
              .mapToObj(i -> createEndedSpan(prefixSpanName + i))
              .filter(Objects::nonNull)
              .collect(toList());

      if (samplingProbability >= 1.) {
        Assertions.assertThat(spans).hasSize(numberOfSpans);
      }

      spanProcessor.forceFlush().join(1000, TimeUnit.SECONDS);

      List<SpanData> exported = spanExporter.getExported();
      Assertions.assertThat(exported).hasSize(Math.min(reservoirSize, spans.size()));

      double totalAdjustedCount = 0;
      for (SpanData spanData : exported) {
        String traceStateString =
            spanData.getSpanContext().getTraceState().get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState traceState = OtelTraceState.parse(traceStateString);
        assertTrue(traceState.hasValidR());
        assertTrue(traceState.hasValidP());
        observedPvalues.merge(traceState.getP(), 1L, Long::sum);
        totalAdjustedCount += Math.pow(2., traceState.getP());
        spanNameCounts.merge(spanData.getName().split("/")[2], 1L, Long::sum);
      }
      totalAdjustedCounts[k] = totalAdjustedCount;
    }

    long totalNumberOfSpans = numberOfSpans * (long) numCycles;
    if (numCycles == 1) {
      Assertions.assertThat(observedPvalues).hasSizeLessThanOrEqualTo(2);
    }
    if (tests.contains(Tests.VERIFY_MEAN)) {
      Assertions.assertThat(reservoirSize)
          .isGreaterThanOrEqualTo(
              100); // require a lower limit on the reservoir size, to justify the assumption of the
      // t-test that values are normally distributed

      Assertions.assertThat(
              new TTest().tTest(totalNumberOfSpans / (double) numCycles, totalAdjustedCounts))
          .isGreaterThan(0.01);
    }
    if (tests.contains(Tests.VERIFY_PVALUE_DISTRIBUTION)) {
      Assertions.assertThat(observedPvalues)
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
      Assertions.assertThat(p1).isLessThanOrEqualTo(p2);

      double effectiveSamplingProbability =
          samplingProbability * p1 + (reservoirSize / (double) numberOfSpans) * (1. - p2);
      verifyObservedPvaluesUsingGtest(
          totalNumberOfSpans, observedPvalues, effectiveSamplingProbability);
    }
    if (tests.contains(Tests.VERIFY_ORDER_INDEPENDENCE)) {
      Assertions.assertThat(spanNameCounts.size()).isEqualTo(numberOfSpans);
      long[] observed = spanNameCounts.values().stream().mapToLong(x -> x).toArray();
      double[] expected = new double[numberOfSpans];
      Arrays.fill(expected, 1.);
      Assertions.assertThat(new GTest().gTest(expected, observed)).isGreaterThan(0.01);
    }
  }

  private void testConsistentSampling(boolean useAlternativeReservoirImplementation) {
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0x34e7052af91d5355L,
        10000,
        1000,
        100,
        1.,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xcd02a41e10ff273dL,
        10000,
        1000,
        100,
        0.8,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0x2c3d086534e14407L,
        10000,
        1000,
        100,
        0.1,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xd3f8a40433cf0522L,
        10000,
        1000,
        200,
        0.9,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xf25638ca67eceadcL,
        10000,
        100,
        100,
        1.0,
        EnumSet.of(Tests.VERIFY_MEAN));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0x14c5f8f815618ce2L,
        10000,
        200,
        100,
        1.0,
        EnumSet.of(Tests.VERIFY_MEAN, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xb6c27f1169e128ddL,
        10000,
        1000,
        200,
        0.2,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xab558ff7c5c73c18L,
        1000,
        10000,
        200,
        1.,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xe53010c4b009a6c0L,
        10000,
        1000,
        2000,
        0.2,
        EnumSet.of(
            Tests.VERIFY_MEAN, Tests.VERIFY_PVALUE_DISTRIBUTION, Tests.VERIFY_ORDER_INDEPENDENCE));
    testConsistentSampling(
        useAlternativeReservoirImplementation,
        0xc41d327fd1a6866aL,
        1000000,
        5,
        4,
        1.0,
        EnumSet.of(Tests.VERIFY_ORDER_INDEPENDENCE));
  }

  @Test
  void testConsistentSampling() {
    testConsistentSampling(false);
  }

  @Test
  void testConsistentSamplingWithAlternativeReservoirImplementation() {
    testConsistentSampling(true);
  }

  private StatisticalSummary calculateStatisticalSummary(
      boolean useAlternativeReservoirImplementation,
      long seed,
      int numCycles,
      int numberOfSpans,
      int reservoirSize,
      double samplingProbability) {

    SplittableRandom rng1 = new SplittableRandom(seed);
    SplittableRandom rng2 = rng1.split();
    RandomGenerator threadSafeRandomGenerator1 =
        () -> {
          synchronized (rng1) {
            return rng1.nextLong();
          }
        };
    RandomGenerator threadSafeRandomGenerator2 =
        () -> {
          synchronized (rng2) {
            return rng1.nextLong();
          }
        };

    WaitingSpanExporter spanExporter =
        new WaitingSpanExporter(0, CompletableResultCode.ofSuccess());

    ConsistentReservoirSamplingBatchSpanProcessor spanProcessor =
        ConsistentReservoirSamplingBatchSpanProcessor.builder(spanExporter)
            .setReservoirSize(reservoirSize)
            .setThreadSafeRandomGenerator(threadSafeRandomGenerator1)
            .setScheduleDelay(1000, TimeUnit.SECONDS)
            .setReservoirSize(reservoirSize)
            .useAlternativeReservoirImplementation(useAlternativeReservoirImplementation)
            .build();

    sdkTracerProvider =
        SdkTracerProvider.builder()
            .setSampler(
                ConsistentSampler.probabilityBased(samplingProbability, threadSafeRandomGenerator2))
            .addSpanProcessor(spanProcessor)
            .build();

    StreamingStatistics streamingStatistics = new StreamingStatistics();

    for (int k = 0; k < numCycles; ++k) {
      String prefixSpanName = "MySpanName/" + k + "/";
      List<ReadableSpan> spans =
          LongStream.range(0, numberOfSpans)
              .mapToObj(i -> createEndedSpan(prefixSpanName + i))
              .filter(Objects::nonNull)
              .collect(toList());

      if (samplingProbability >= 1.) {
        Assertions.assertThat(spans).hasSize(numberOfSpans);
      }

      spanProcessor.forceFlush().join(1000, TimeUnit.SECONDS);

      List<SpanData> exported = spanExporter.getExported();
      Assertions.assertThat(exported).hasSize(Math.min(reservoirSize, spans.size()));

      double totalAdjustedCount = 0;
      for (SpanData spanData : exported) {
        String traceStateString =
            spanData.getSpanContext().getTraceState().get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState traceState = OtelTraceState.parse(traceStateString);
        assertTrue(traceState.hasValidR());
        assertTrue(traceState.hasValidP());
        totalAdjustedCount += Math.pow(2., traceState.getP());
      }
      streamingStatistics.accept(totalAdjustedCount);
    }
    return streamingStatistics;
  }

  @Test
  void testVarianceDifferencesBetweenReservoirImplementations1() {
    boolean useAlternativeReservoirImplementationFalse = false;
    boolean useAlternativeReservoirImplementationTrue = true;

    StatisticalSummary variant1 =
        calculateStatisticalSummary(
            useAlternativeReservoirImplementationFalse, 0x225de9590067d658L, 10000, 100, 50, 0.8);

    StatisticalSummary variant2 =
        calculateStatisticalSummary(
            useAlternativeReservoirImplementationTrue, 0x23b418ed68d668L, 10000, 100, 50, 0.8);

    Assertions.assertThat(variant1.getMean()).isCloseTo(100, Percentage.withPercentage(1));
    Assertions.assertThat(variant2.getMean()).isCloseTo(100, Percentage.withPercentage(1));

    Assertions.assertThat(variant1.getVariance())
        .isCloseTo(111.17153690368997, Percentage.withPercentage(0.01));
    Assertions.assertThat(variant2.getVariance())
        .isCloseTo(95.65024978497851, Percentage.withPercentage(0.01));
  }

  @Test
  void testVarianceDifferencesBetweenReservoirImplementations2() {
    boolean useAlternativeReservoirImplementationFalse = false;
    boolean useAlternativeReservoirImplementationTrue = true;

    StatisticalSummary variant1 =
        calculateStatisticalSummary(
            useAlternativeReservoirImplementationFalse, 0x7f33baf84d59df65L, 100000, 10, 4, 0.9);

    StatisticalSummary variant2 =
        calculateStatisticalSummary(
            useAlternativeReservoirImplementationTrue, 0xdb3bf0109e0a4b43L, 100000, 10, 4, 0.9);

    Assertions.assertThat(variant1.getMean()).isCloseTo(10, Percentage.withPercentage(2));
    Assertions.assertThat(variant2.getMean()).isCloseTo(10, Percentage.withPercentage(2));
    Assertions.assertThat(variant1.getVariance())
        .isCloseTo(22.617777121371127, Percentage.withPercentage(0.01));
    Assertions.assertThat(variant2.getVariance())
        .isCloseTo(19.465000847508666, Percentage.withPercentage(0.01));
  }

  @Test
  void testVarianceDifferencesBetweenReservoirImplementations3() {
    boolean useAlternativeReservoirImplementationFalse = false;
    boolean useAlternativeReservoirImplementationTrue = true;

    StatisticalSummary variant1 =
        calculateStatisticalSummary(
            useAlternativeReservoirImplementationFalse, 0x72d7312adac9c84dL, 10000, 1000, 700, 1);

    StatisticalSummary variant2 =
        calculateStatisticalSummary(
            useAlternativeReservoirImplementationTrue, 0x7ea0c32d80a319d0L, 10000, 1000, 700, 1.);

    Assertions.assertThat(variant1.getMean()).isCloseTo(1000, Percentage.withPercentage(1));
    Assertions.assertThat(variant2.getMean()).isCloseTo(1000, Percentage.withPercentage(1));
    Assertions.assertThat(variant1.getVariance())
        .isCloseTo(594.9523749875004, Percentage.withPercentage(0.01));
    Assertions.assertThat(variant2.getVariance())
        .isCloseTo(362.2863018701875, Percentage.withPercentage(0.01));
  }
}
