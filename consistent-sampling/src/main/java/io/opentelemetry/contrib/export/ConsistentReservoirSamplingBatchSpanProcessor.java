/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.export;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.state.OtelTraceState;
import io.opentelemetry.contrib.util.RandomGenerator;
import io.opentelemetry.contrib.util.RandomUtil;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link SpanProcessor} that batches spans exported by the SDK then pushes
 * them to the exporter pipeline.
 *
 * <p>All spans reported by the SDK implementation are first added to a synchronized queue (with a
 * {@code maxQueueSize} maximum size, if queue is full spans are dropped). Spans are exported either
 * when there are {@code maxExportBatchSize} pending spans or {@code scheduleDelayNanos} has passed
 * since the last export finished.
 */
public final class ConsistentReservoirSamplingBatchSpanProcessor implements SpanProcessor {

  private static final String WORKER_THREAD_NAME =
      ConsistentReservoirSamplingBatchSpanProcessor.class.getSimpleName() + "_WorkerThread";
  private static final AttributeKey<String> SPAN_PROCESSOR_TYPE_LABEL =
      AttributeKey.stringKey("spanProcessorType");
  private static final AttributeKey<Boolean> SPAN_PROCESSOR_DROPPED_LABEL =
      AttributeKey.booleanKey("dropped");
  private static final String SPAN_PROCESSOR_TYPE_VALUE =
      ConsistentReservoirSamplingBatchSpanProcessor.class.getSimpleName();

  private final Worker worker;
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  /**
   * Returns a new Builder for {@link ConsistentReservoirSamplingBatchSpanProcessor}.
   *
   * @param spanExporter the {@code SpanExporter} to where the Spans are pushed.
   * @return a new {@link ConsistentReservoirSamplingBatchSpanProcessor}.
   * @throws NullPointerException if the {@code spanExporter} is {@code null}.
   */
  public static ConsistentReservoirSamplingBatchSpanProcessorBuilder builder(
      SpanExporter spanExporter) {
    return new ConsistentReservoirSamplingBatchSpanProcessorBuilder(spanExporter);
  }

  private static final class ReadableSpanWithPriority {

    private final ReadableSpan readableSpan;
    private int pval;
    private final int rval;
    private long priority;

    public static ReadableSpanWithPriority create(
        ReadableSpan readableSpan, RandomGenerator threadSafeRandomGenerator) {
      String otelTraceStateString =
          readableSpan.getSpanContext().getTraceState().get(OtelTraceState.TRACE_STATE_KEY);
      OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);
      int pval;
      int rval;
      long priority = threadSafeRandomGenerator.nextLong();
      if (otelTraceState.hasValidR()) {
        rval = otelTraceState.getR();
      } else {
        rval =
            Math.min(
                threadSafeRandomGenerator.numberOfLeadingZerosOfRandomLong(),
                OtelTraceState.getMaxR());
      }

      if (otelTraceState.hasValidP()) {
        pval = otelTraceState.getP();
      } else {
        // if the p-value is not defined assume it is zero,
        // which corresponds to an adjusted count of 1
        pval = 0;
      }

      return new ReadableSpanWithPriority(readableSpan, pval, rval, priority);
    }

    private ReadableSpanWithPriority(ReadableSpan readableSpan, int pval, int rval, long priority) {
      this.readableSpan = readableSpan;
      this.pval = pval;
      this.rval = rval;
      this.priority = priority;
    }

    private ReadableSpan getReadableSpan() {
      return readableSpan;
    }

    private int getP() {
      return pval;
    }

    private void setP(int pval) {
      this.pval = pval;
    }

    private int getR() {
      return rval;
    }

    // returns true if this span survived down sampling
    private boolean downSample(RandomGenerator threadSafeRandomGenerator) {
      pval += 1;
      if (pval > rval) {
        return false;
      }
      priority = threadSafeRandomGenerator.nextLong();
      return true;
    }

    private static int comparePthenPriority(
        ReadableSpanWithPriority s1, ReadableSpanWithPriority s2) {
      int compareP = Integer.compare(s1.pval, s2.pval);
      if (compareP != 0) {
        return compareP;
      }
      return Long.compare(s1.priority, s2.priority);
    }

    private static int compareRthenPriority(
        ReadableSpanWithPriority s1, ReadableSpanWithPriority s2) {
      int compareR = Integer.compare(s1.rval, s2.rval);
      if (compareR != 0) {
        return compareR;
      }
      return Long.compare(s1.priority, s2.priority);
    }
  }

  private interface Reservoir {
    void add(ReadableSpanWithPriority readableSpanWithPriority);

    List<SpanData> getResult();

    boolean isEmpty();
  }

  /**
   * Reservoir sampling buffer that collects a fixed number of spans.
   *
   * <p>Consistent sampling requires that spans are sampled only if r-value >= p-value, where
   * p-value describes which sampling rate from the discrete set of possible sampling rates is
   * applied. Consistent sampling allows to choose the sampling rate (the p-value) individually for
   * every span. Therefore, the number of sampled spans can be reduced by increasing the p-value of
   * spans, such that spans for which r-value < p-value get discarded. To reduce the number of
   * sampled spans one can therefore apply the following procedure until the desired number of spans
   * are left:
   *
   * <p>1) Randomly choose a span among the spans with smallest p-values
   *
   * <p>2) Increment its p-value by 1
   *
   * <p>3) Discard the span, if r-value < p-value
   *
   * <p>4) continue with 1)
   *
   * <p>By always incrementing one of the smallest p-values, this approach tries to balance the
   * sampling rates (p-values). Balanced sampling rates are better for estimation (compare VarOpt
   * sampling, see https://arxiv.org/abs/0803.0473).
   *
   * <p>This reservoir sampling approach implements the described procedure in a streaming fashion.
   * In order to ensure that spans have fair chances regardless of processing order, a uniform
   * random number (priority) is associated with its p-value. When choosing a span among the spans
   * with smallest p-value, we take that with the smallest priority.
   */
  private static final class Reservoir1 implements Reservoir {
    private final int reservoirSize;
    private final PriorityQueue<ReadableSpanWithPriority> queue;
    private final RandomGenerator threadSafeRandomGenerator;

    public Reservoir1(int reservoirSize, RandomGenerator threadSafeRandomGenerator) {
      if (reservoirSize < 1) {
        throw new IllegalArgumentException();
      }
      this.reservoirSize = reservoirSize;
      this.queue =
          new PriorityQueue<>(reservoirSize, ReadableSpanWithPriority::comparePthenPriority);
      this.threadSafeRandomGenerator = threadSafeRandomGenerator;
    }

    @Override
    public void add(ReadableSpanWithPriority readableSpanWithPriority) {
      if (queue.size() < reservoirSize) {
        queue.add(readableSpanWithPriority);
        return;
      }

      do {
        ReadableSpanWithPriority head = queue.peek();
        if (ReadableSpanWithPriority.comparePthenPriority(readableSpanWithPriority, head) > 0) {
          queue.remove();
          queue.add(readableSpanWithPriority);
          readableSpanWithPriority = head;
        }
      } while (readableSpanWithPriority.downSample(threadSafeRandomGenerator));
    }

    @Override
    public List<SpanData> getResult() {
      List<SpanData> result = new ArrayList<>(queue.size());
      for (ReadableSpanWithPriority readableSpanWithPriority : queue) {
        SpanData spanData = readableSpanWithPriority.getReadableSpan().toSpanData();
        SpanContext spanContext = spanData.getSpanContext();
        TraceState traceState = spanContext.getTraceState();
        String otelTraceStateString = traceState.get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);
        if ((!otelTraceState.hasValidR() && readableSpanWithPriority.getP() > 0)
            || (otelTraceState.hasValidR()
                && readableSpanWithPriority.getP() != otelTraceState.getP())) {
          otelTraceState.setP(readableSpanWithPriority.getP());
          spanData = updateSpanDataWithOtelTraceState(spanData, otelTraceState);
        }
        result.add(spanData);
      }
      return result;
    }

    @Override
    public boolean isEmpty() {
      return queue.isEmpty();
    }
  }

  /**
   * This reservoir implementation is (almost) statistically equivalent to {@link Reservoir1}.
   *
   * <p>It uses a priority queue where the minimum is the span with the smallet r-value. In this way
   * the add-operation is more efficient, and has a worst case time complexity of O(log n) where n
   * denotes the reservoir size.
   */
  private static final class Reservoir2 implements Reservoir {
    private final int reservoirSize;
    private int maxDiscardedRValue = 0;
    private long numberOfDiscardedSpansWithMaxDiscardedRValue = 0;
    private final PriorityQueue<ReadableSpanWithPriority> queue;
    private final RandomGenerator threadSafeRandomGenerator;

    public Reservoir2(int reservoirSize, RandomGenerator threadSafeRandomGenerator) {
      if (reservoirSize < 1) {
        throw new IllegalArgumentException();
      }
      this.reservoirSize = reservoirSize;
      this.queue =
          new PriorityQueue<>(reservoirSize, ReadableSpanWithPriority::compareRthenPriority);
      this.threadSafeRandomGenerator = threadSafeRandomGenerator;
    }

    @Override
    public void add(ReadableSpanWithPriority readableSpanWithPriority) {

      if (queue.size() < reservoirSize) {
        queue.add(readableSpanWithPriority);
        return;
      }

      ReadableSpanWithPriority head = queue.peek();
      if (ReadableSpanWithPriority.compareRthenPriority(readableSpanWithPriority, head) > 0) {
        queue.remove();
        queue.add(readableSpanWithPriority);
        readableSpanWithPriority = head;
      }
      if (readableSpanWithPriority.getR() > maxDiscardedRValue) {
        maxDiscardedRValue = readableSpanWithPriority.getR();
        numberOfDiscardedSpansWithMaxDiscardedRValue = 1;
      } else if (readableSpanWithPriority.getR() == maxDiscardedRValue) {
        numberOfDiscardedSpansWithMaxDiscardedRValue += 1;
      }
    }

    @Override
    public List<SpanData> getResult() {

      if (numberOfDiscardedSpansWithMaxDiscardedRValue == 0) {
        return queue.stream().map(x -> x.readableSpan.toSpanData()).collect(Collectors.toList());
      }

      List<ReadableSpanWithPriority> readableSpansWithPriority = new ArrayList<>(queue.size());
      int numberOfSampledSpansWithMaxDiscardedRValue = 0;
      int numSampledSpansWithGreaterRValueAndSmallPValue = 0;
      for (ReadableSpanWithPriority readableSpanWithPriority : queue) {
        if (readableSpanWithPriority.getR() == maxDiscardedRValue) {
          numberOfSampledSpansWithMaxDiscardedRValue += 1;
        } else if (readableSpanWithPriority.getP() <= maxDiscardedRValue) {
          numSampledSpansWithGreaterRValueAndSmallPValue += 1;
        }
        readableSpansWithPriority.add(readableSpanWithPriority);
      }

      // Z = reservoirSize
      // L = maxDiscardedRValue
      // R = numberOfDiscardedSpansWithMaxDiscardedRValue
      // K = numSampledSpansWithGreaterRValueAndSmallPValue
      // X = numberOfSampledSpansWithMaxDiscardedRValue
      //
      // The sampling approach described above for Reservoir1 can be equivalently performed by
      // keeping Z spans with largest r-values (in case of ties with highest priority) and adjusting
      // the p-values at the end. We know that the largest r-value among the dropped spans is L and
      // that we had to discard exactly R spans with (r-value == L). This implies that their
      // corresponding p-values were raised to (L + 1) which finally violated the sampling condition
      // (r-value >= p-value). We only raise the p-value of some span if it belongs to the set of
      // spans with minimum p-value. Therefore, the minimum p-value must be given by L. To determine
      // the p-values of all kept spans, we consider 3 cases:
      //
      // 1) For all X kept spans with r-value == L the corresponding p-value must also be L.
      // Otherwise, the span would have been discarded. There are R spans with (r-value == L) which
      // have been discarded. Therefore, among the original (X + R) spans with (r-value == L) we
      // have kept X spans.
      //
      // 2) For spans with (p-value > L) the p-value will not be changed as they do not belong to
      // the set of spans with minimal p-values.
      //
      // 3) For the remaining K spans for which (r-value > L) and (p-value <= L) the p-value needs
      // to be adjusted. The new p-value will be either L or (L + 1). When starting to sample the
      // first spans with (p-value == L), we have N = R + K + X spans which all have (r-value >= L)
      // and (p-value == L). This set can be divided into two sets of spans dependent on whether
      // (r-value == L) or (r-value > L). We know that there were (R + X) spans with (r-value == L)
      // and K spans with (r-value > L). When randomly selecting a span to increase its p-value, the
      // span will only be discarded if the span belongs to the first set (r-value == L). We will
      // call such an event "failure". If the selected span belongs to the second set (r-value > L),
      // its p-value will be increased by 1 to (L + 1) but the span will not be dropped. The
      // sampling procedure will be stopped after R "failures". The number of "successes" follows a
      // negative hypergeometric distribution
      // (see https://en.wikipedia.org/wiki/Negative_hypergeometric_distribution).
      // Therefore, we need to sample a random value from a negative hypergeometric distribution
      // with N = R + X + K elements of which K are "successes" and after drawing R "failures", in
      // order to determine how many spans out of K will get a p-value equal to (L + 1). The
      // expected number is given by R * K / (N - K + 1) = R * K / (R + X + 1). Instead of drawing
      // the number from the negative hypergeometric distribution we could also set it to the
      // stochastically rounded expected value. This makes this reservoir sampling approach not
      // fully equivalent to the approach described above for Reservoir1, but this (probably) leads
      // to a smaller variance when it comes to estimation. (TODO: This still has to be verified!)

      double expectedNumPValueIncrements =
          numSampledSpansWithGreaterRValueAndSmallPValue
              * (numberOfDiscardedSpansWithMaxDiscardedRValue
                  / (double)
                      (numberOfDiscardedSpansWithMaxDiscardedRValue
                          + numberOfSampledSpansWithMaxDiscardedRValue
                          + 1L));
      int roundedExpectedNumPValueIncrements =
          Math.toIntExact(
              RandomUtil.roundStochastically(
                  threadSafeRandomGenerator, expectedNumPValueIncrements));

      BitSet incrementIndicators =
          RandomUtil.generateRandomBitSet(
              threadSafeRandomGenerator,
              numSampledSpansWithGreaterRValueAndSmallPValue,
              roundedExpectedNumPValueIncrements);

      int incrementIndicatorIndex = 0;
      List<SpanData> result = new ArrayList<>(queue.size());
      for (ReadableSpanWithPriority readableSpanWithPriority : readableSpansWithPriority) {
        if (readableSpanWithPriority.getP() <= maxDiscardedRValue) {
          readableSpanWithPriority.setP(maxDiscardedRValue);
          if (readableSpanWithPriority.getR() > maxDiscardedRValue) {
            if (incrementIndicators.get(incrementIndicatorIndex)) {
              readableSpanWithPriority.setP(maxDiscardedRValue + 1);
            }
            incrementIndicatorIndex += 1;
          }
        }

        SpanData spanData = readableSpanWithPriority.getReadableSpan().toSpanData();
        SpanContext spanContext = spanData.getSpanContext();
        TraceState traceState = spanContext.getTraceState();
        String otelTraceStateString = traceState.get(OtelTraceState.TRACE_STATE_KEY);
        OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);
        if ((!otelTraceState.hasValidR() && readableSpanWithPriority.getP() > 0)
            || (otelTraceState.hasValidR()
                && readableSpanWithPriority.getP() != otelTraceState.getP())) {
          otelTraceState.setP(readableSpanWithPriority.getP());
          spanData = updateSpanDataWithOtelTraceState(spanData, otelTraceState);
        }
        result.add(spanData);
      }

      return result;
    }

    @Override
    public boolean isEmpty() {
      return queue.isEmpty();
    }
  }

  private static SpanData updateSpanDataWithOtelTraceState(
      SpanData spanData, OtelTraceState otelTraceState) {
    SpanContext spanContext = spanData.getSpanContext();
    TraceState traceState = spanContext.getTraceState();
    String updatedOtelTraceStateString = otelTraceState.serialize();
    TraceState updatedTraceState =
        traceState.toBuilder()
            .put(OtelTraceState.TRACE_STATE_KEY, updatedOtelTraceStateString)
            .build();
    SpanContext updatedSpanContext =
        SpanContext.create(
            spanContext.getTraceId(),
            spanContext.getSpanId(),
            spanContext.getTraceFlags(),
            updatedTraceState);
    return new DelegatingSpanData(spanData) {
      @Override
      public SpanContext getSpanContext() {
        return updatedSpanContext;
      }
    };
  }

  ConsistentReservoirSamplingBatchSpanProcessor(
      SpanExporter spanExporter,
      MeterProvider meterProvider,
      long scheduleDelayNanos,
      int reservoirSize,
      long exporterTimeoutNanos,
      RandomGenerator threadSafeRandomGenerator,
      boolean useAlternativeReservoirImplementation) {
    this.worker =
        new Worker(
            spanExporter,
            meterProvider,
            scheduleDelayNanos,
            reservoirSize,
            exporterTimeoutNanos,
            threadSafeRandomGenerator,
            useAlternativeReservoirImplementation);
    Thread workerThread = new DaemonThreadFactory(WORKER_THREAD_NAME).newThread(worker);
    workerThread.start();
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    if (span == null || !span.getSpanContext().isSampled()) {
      return;
    }
    worker.addSpan(span);
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  @Override
  public CompletableResultCode shutdown() {
    if (isShutdown.getAndSet(true)) {
      return CompletableResultCode.ofSuccess();
    }
    return worker.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return worker.forceFlush();
  }

  // Visible for testing
  boolean isReservoirEmpty() {
    return worker.isReservoirEmpty();
  }

  private static final class Worker implements Runnable {

    private final LongCounter processedSpansCounter;
    private final Attributes droppedAttrs;
    private final Attributes exportedAttrs;
    private final boolean useAlternativeReservoirImplementation;

    private static final Logger logger = Logger.getLogger(Worker.class.getName());
    private final SpanExporter spanExporter;
    private final long scheduleDelayNanos;
    private final int reservoirSize;
    private final long exporterTimeoutNanos;

    private long nextExportTime;

    private final RandomGenerator threadSafeRandomGenerator;
    private final Object reservoirLock = new Object();
    private Reservoir reservoir;
    private final BlockingQueue<CompletableResultCode> signal;
    private volatile boolean continueWork = true;

    private static Reservoir createReservoir(
        int reservoirSize,
        boolean useAlternativeReservoirImplementation,
        RandomGenerator threadSafeRandomGenerator) {
      if (useAlternativeReservoirImplementation) {
        return new Reservoir2(reservoirSize, threadSafeRandomGenerator);
      } else {
        return new Reservoir1(reservoirSize, threadSafeRandomGenerator);
      }
    }

    private Worker(
        SpanExporter spanExporter,
        MeterProvider meterProvider,
        long scheduleDelayNanos,
        int reservoirSize,
        long exporterTimeoutNanos,
        RandomGenerator threadSafeRandomGenerator,
        boolean useAlternativeReservoirImplementation) {
      this.useAlternativeReservoirImplementation = useAlternativeReservoirImplementation;
      this.spanExporter = spanExporter;
      this.scheduleDelayNanos = scheduleDelayNanos;
      this.reservoirSize = reservoirSize;
      this.exporterTimeoutNanos = exporterTimeoutNanos;
      this.threadSafeRandomGenerator = threadSafeRandomGenerator;
      synchronized (reservoirLock) {
        this.reservoir =
            createReservoir(
                reservoirSize, useAlternativeReservoirImplementation, threadSafeRandomGenerator);
      }
      this.signal = new ArrayBlockingQueue<>(1);
      Meter meter = meterProvider.meterBuilder("io.opentelemetry.sdk.trace").build();
      processedSpansCounter =
          meter
              .counterBuilder("processedSpans")
              .setUnit("1")
              .setDescription(
                  "The number of spans processed by the BatchSpanProcessor. "
                      + "[dropped=true if they were dropped due to high throughput]")
              .build();
      droppedAttrs =
          Attributes.of(
              SPAN_PROCESSOR_TYPE_LABEL,
              SPAN_PROCESSOR_TYPE_VALUE,
              SPAN_PROCESSOR_DROPPED_LABEL,
              true);
      exportedAttrs =
          Attributes.of(
              SPAN_PROCESSOR_TYPE_LABEL,
              SPAN_PROCESSOR_TYPE_VALUE,
              SPAN_PROCESSOR_DROPPED_LABEL,
              false);
    }

    private void addSpan(ReadableSpan span) {
      ReadableSpanWithPriority readableSpanWithPriority =
          ReadableSpanWithPriority.create(span, threadSafeRandomGenerator);
      synchronized (reservoirLock) {
        reservoir.add(readableSpanWithPriority);
      }
      processedSpansCounter.add(1, droppedAttrs);
    }

    @Override
    public void run() {
      updateNextExportTime();
      CompletableResultCode completableResultCode = null;
      while (continueWork) {

        if (completableResultCode != null || System.nanoTime() >= nextExportTime) {
          Reservoir oldReservoir;
          Reservoir newReservoir =
              createReservoir(
                  reservoirSize, useAlternativeReservoirImplementation, threadSafeRandomGenerator);
          synchronized (reservoirLock) {
            oldReservoir = reservoir;
            reservoir = newReservoir;
          }
          exportCurrentBatch(oldReservoir.getResult());
          updateNextExportTime();
          if (completableResultCode != null) {
            completableResultCode.succeed();
          }
        }

        try {
          long pollWaitTime = nextExportTime - System.nanoTime();
          if (pollWaitTime > 0) {
            completableResultCode = signal.poll(pollWaitTime, TimeUnit.NANOSECONDS);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    private void updateNextExportTime() {
      nextExportTime = System.nanoTime() + scheduleDelayNanos;
    }

    private CompletableResultCode shutdown() {
      CompletableResultCode result = new CompletableResultCode();

      CompletableResultCode flushResult = forceFlush();
      flushResult.whenComplete(
          () -> {
            continueWork = false;
            CompletableResultCode shutdownResult = spanExporter.shutdown();
            shutdownResult.whenComplete(
                () -> {
                  if (!flushResult.isSuccess() || !shutdownResult.isSuccess()) {
                    result.fail();
                  } else {
                    result.succeed();
                  }
                });
          });

      return result;
    }

    private CompletableResultCode forceFlush() {
      CompletableResultCode flushResult = new CompletableResultCode();
      signal.offer(flushResult);
      return flushResult;
    }

    private void exportCurrentBatch(List<SpanData> batch) {
      if (batch.isEmpty()) {
        return;
      }

      try {
        CompletableResultCode result = spanExporter.export(Collections.unmodifiableList(batch));
        result.join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
        if (result.isSuccess()) {
          processedSpansCounter.add(batch.size(), exportedAttrs);
        } else {
          logger.log(Level.FINE, "Exporter failed");
        }
      } catch (RuntimeException e) {
        logger.log(Level.WARNING, "Exporter threw an Exception", e);
      } finally {
        batch.clear();
      }
    }

    private boolean isReservoirEmpty() {
      synchronized (reservoirLock) {
        return reservoir.isEmpty();
      }
    }
  }
}
