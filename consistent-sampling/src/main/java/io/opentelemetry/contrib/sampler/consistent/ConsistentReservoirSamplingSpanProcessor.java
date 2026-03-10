/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.internal.DaemonThreadFactory;
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
 * A {@link SpanProcessor} which periodically exports a fixed maximum number of spans. If the number
 * of spans in a period exceeds the fixed reservoir (buffer) size, spans will be consistently
 * (compare {@link ConsistentSampler}) sampled.
 */
public final class ConsistentReservoirSamplingSpanProcessor implements SpanProcessor {

  private static final String WORKER_THREAD_NAME =
      ConsistentReservoirSamplingSpanProcessor.class.getSimpleName() + "_WorkerThread";

  private final Worker worker;
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  // visible for testing
  static final long DEFAULT_EXPORT_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

  private static final class ReadableSpanWithPriority {

    private final ReadableSpan readableSpan;
    private int pval;
    private final int rval;
    private final long priority;

    static ReadableSpanWithPriority create(
        ReadableSpan readableSpan, RandomGenerator randomGenerator) {
      String otelTraceStateString =
          readableSpan.getSpanContext().getTraceState().get(OtelTraceState.TRACE_STATE_KEY);
      OtelTraceState otelTraceState = OtelTraceState.parse(otelTraceStateString);
      int pval;
      int rval;
      long priority = randomGenerator.nextLong();
      if (otelTraceState.hasValidR()) {
        rval = otelTraceState.getR();
      } else {
        rval =
            Math.min(randomGenerator.numberOfLeadingZerosOfRandomLong(), OtelTraceState.getMaxR());
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

    private static int compareRthenPriority(
        ReadableSpanWithPriority s1, ReadableSpanWithPriority s2) {
      int compareR = Integer.compare(s1.rval, s2.rval);
      if (compareR != 0) {
        return compareR;
      }
      return Long.compare(s1.priority, s2.priority);
    }
  }

  /**
   * A reservoir sampling buffer that collects a fixed number of spans.
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
   * sampling rates (p-values). Balanced sampling rates are better for estimation (compare <a
   * href="https://arxiv.org/abs/0803.0473">VarOpt sampling</a>).
   *
   * <p>This sampling approach can be implemented in a streaming fashion. In order to ensure that
   * spans have fair chances regardless of processing order, a uniform random number (priority) is
   * associated with its p-value. When choosing a span among all spans with smallest p-value, we
   * take that with the smallest priority. For that, a priority queue is needed.
   *
   * <p>In the following, an equivalent and more efficient sampling approach is described, that is
   * based on a priority queue where the minimum is the span with the smallest r-value. In this way
   * the {@code add}-operation will have a worst case time complexity of {@code O(log n)} where
   * {@code n} denotes the reservoir size. We use the following notation:
   *
   * <p>Z := {@code reservoirSize}
   *
   * <p>L := {@code maxDiscardedRValue}
   *
   * <p>R := {@code numberOfDiscardedSpansWithMaxDiscardedRValue}
   *
   * <p>K := {@code numSampledSpansWithGreaterRValueAndSmallPValue}
   *
   * <p>X := {@code numberOfSampledSpansWithMaxDiscardedRValue}
   *
   * <p>The sampling approach described above can be equivalently performed by keeping Z spans with
   * largest r-values (in case of ties with highest priority) and adjusting the corresponding
   * p-values in a finalization step. We know that the largest r-value among the dropped spans is L
   * and that we had to discard exactly R spans with (r-value == L). This implies that their
   * corresponding p-values were raised to (L + 1) which finally violated the sampling condition
   * (r-value >= p-value). We only raise the p-value of some span, if it belongs to the set of spans
   * with minimum p-value. Therefore, the minimum p-value must be given by L. To determine the
   * p-values of all finally kept spans, we consider 3 cases:
   *
   * <p>1) For all X kept spans with r-value == L the corresponding p-value must also be L.
   * Otherwise, the span would have been discarded. There are R spans with (r-value == L) which have
   * been discarded. Therefore, among the original (X + R) spans with (r-value == L) we have kept X
   * spans.
   *
   * <p>2) For spans with (p-value > L) the p-value will not be changed as they do not belong to the
   * set of spans with minimal p-values.
   *
   * <p>3) For the remaining K spans for which (r-value > L) and (p-value <= L) the p-value needs to
   * be adjusted. The new p-value will be either L or (L + 1). When starting to sample the first
   * spans with (p-value == L), we have N = R + K + X spans which all have (r-value >= L) and
   * (p-value == L). This set can be divided into two sets of spans dependent on whether (r-value ==
   * L) or (r-value > L). We know that there were (R + X) spans with (r-value == L) and K spans with
   * (r-value > L). When randomly selecting a span to increase its p-value, the span will only be
   * discarded if the span belongs to the first set (r-value == L). We will call such an event
   * "failure". If the selected span belongs to the second set (r-value > L), its p-value will be
   * increased by 1 to (L + 1) but the span will not be dropped. The sampling procedure will be
   * stopped after R "failures". The number of "successes" follows a <a
   * href="https://en.wikipedia.org/wiki/Negative_hypergeometric_distribution">negative
   * hypergeometric distribution</a>. Therefore, we need to sample a random value from a negative
   * hypergeometric distribution with N = R + X + K elements of which K are "successes" and after
   * drawing R "failures", in order to determine how many spans out of K will get a p-value equal to
   * (L + 1). The expected number is given by R * K / (N - K + 1) = R * K / (R + X + 1). Instead of
   * drawing the number from the negative hypergeometric distribution we could also set it to the
   * stochastically rounded expected value. This makes this reservoir sampling approach not fully
   * equivalent to the approach described initially, but leads to a smaller variance when
   * estimating.
   */
  private static final class Reservoir {
    private final int reservoirSize;
    private int maxDiscardedRValue = 0;
    private long numberOfDiscardedSpansWithMaxDiscardedRValue = 0;
    private final PriorityQueue<ReadableSpanWithPriority> queue;
    private final RandomGenerator randomGenerator;

    Reservoir(int reservoirSize, RandomGenerator randomGenerator) {
      if (reservoirSize < 1) {
        throw new IllegalArgumentException();
      }
      this.reservoirSize = reservoirSize;
      this.queue =
          new PriorityQueue<>(reservoirSize, ReadableSpanWithPriority::compareRthenPriority);
      this.randomGenerator = randomGenerator;
    }

    void add(ReadableSpanWithPriority readableSpanWithPriority) {

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

    List<SpanData> getResult() {

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

      double expectedNumPValueIncrements =
          numSampledSpansWithGreaterRValueAndSmallPValue
              * (numberOfDiscardedSpansWithMaxDiscardedRValue
                  / (double)
                      (numberOfDiscardedSpansWithMaxDiscardedRValue
                          + numberOfSampledSpansWithMaxDiscardedRValue
                          + 1L));
      int roundedExpectedNumPValueIncrements =
          Math.toIntExact(randomGenerator.roundStochastically(expectedNumPValueIncrements));

      BitSet incrementIndicators =
          randomGenerator.generateRandomBitSet(
              numSampledSpansWithGreaterRValueAndSmallPValue, roundedExpectedNumPValueIncrements);

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

    boolean isEmpty() {
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

  // visible for testing
  static SpanProcessor create(
      SpanExporter spanExporter,
      int reservoirSize,
      long exportPeriodNanos,
      long exporterTimeoutNanos,
      RandomGenerator randomGenerator) {
    return new ConsistentReservoirSamplingSpanProcessor(
        spanExporter, exportPeriodNanos, reservoirSize, exporterTimeoutNanos, randomGenerator);
  }

  /**
   * Creates a new {@link SpanProcessor} which periodically exports a fixed maximum number of spans.
   * If the number of spans in a period exceeds the fixed reservoir (buffer) size, spans will be
   * consistently (compare {@link ConsistentSampler}) sampled.
   *
   * @param spanExporter a span exporter
   * @param reservoirSize the reservoir size
   * @param exportPeriodNanos the export period in nanoseconds
   * @param exporterTimeoutNanos the exporter timeout in nanoseconds
   * @return a span processor
   */
  public static SpanProcessor create(
      SpanExporter spanExporter,
      int reservoirSize,
      long exportPeriodNanos,
      long exporterTimeoutNanos) {
    return create(
        spanExporter,
        reservoirSize,
        exportPeriodNanos,
        exporterTimeoutNanos,
        RandomGenerator.getDefault());
  }

  /**
   * Creates a new {@link SpanProcessor} which periodically exports a fixed maximum number of spans.
   * If the number of spans in a period exceeds the fixed reservoir (buffer) size, spans will be
   * consistently (compare {@link ConsistentSampler}) sampled.
   *
   * @param spanExporter a span exporter
   * @param reservoirSize the reservoir size
   * @param exportPeriodNanos the export period in nanoseconds
   * @return a span processor
   */
  static SpanProcessor create(
      SpanExporter spanExporter, int reservoirSize, long exportPeriodNanos) {
    return create(spanExporter, reservoirSize, exportPeriodNanos, DEFAULT_EXPORT_TIMEOUT_NANOS);
  }

  private ConsistentReservoirSamplingSpanProcessor(
      SpanExporter spanExporter,
      long exportPeriodNanos,
      int reservoirSize,
      long exporterTimeoutNanos,
      RandomGenerator randomGenerator) {
    requireNonNull(spanExporter, "spanExporter");
    checkArgument(exportPeriodNanos > 0, "export period must be positive");
    checkArgument(reservoirSize > 0, "reservoir size must be positive");
    checkArgument(exporterTimeoutNanos > 0, "exporter timeout must be positive");
    requireNonNull(randomGenerator, "randomGenerator");

    this.worker =
        new Worker(
            spanExporter, exportPeriodNanos, reservoirSize, exporterTimeoutNanos, randomGenerator);
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

    private static final Logger logger = Logger.getLogger(Worker.class.getName());
    private final SpanExporter spanExporter;
    private final long exportPeriodNanos;
    private final int reservoirSize;
    private final long exporterTimeoutNanos;

    private long nextExportTime;

    private final RandomGenerator randomGenerator;
    private final Object reservoirLock = new Object();
    private Reservoir reservoir;
    private final BlockingQueue<CompletableResultCode> signal;
    private volatile boolean continueWork = true;

    private static Reservoir createReservoir(int reservoirSize, RandomGenerator randomGenerator) {
      return new Reservoir(reservoirSize, randomGenerator);
    }

    private Worker(
        SpanExporter spanExporter,
        long exportPeriodNanos,
        int reservoirSize,
        long exporterTimeoutNanos,
        RandomGenerator randomGenerator) {
      this.spanExporter = spanExporter;
      this.exportPeriodNanos = exportPeriodNanos;
      this.reservoirSize = reservoirSize;
      this.exporterTimeoutNanos = exporterTimeoutNanos;
      this.randomGenerator = randomGenerator;
      synchronized (reservoirLock) {
        this.reservoir = createReservoir(reservoirSize, randomGenerator);
      }
      this.signal = new ArrayBlockingQueue<>(1);
    }

    private void addSpan(ReadableSpan span) {
      ReadableSpanWithPriority readableSpanWithPriority =
          ReadableSpanWithPriority.create(span, randomGenerator);
      synchronized (reservoirLock) {
        reservoir.add(readableSpanWithPriority);
      }
    }

    @Override
    public void run() {
      updateNextExportTime();
      CompletableResultCode completableResultCode = null;
      while (continueWork) {

        if (completableResultCode != null || System.nanoTime() >= nextExportTime) {
          Reservoir oldReservoir;
          Reservoir newReservoir = createReservoir(reservoirSize, randomGenerator);
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
      nextExportTime = System.nanoTime() + exportPeriodNanos;
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
        if (!result.isSuccess()) {
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
