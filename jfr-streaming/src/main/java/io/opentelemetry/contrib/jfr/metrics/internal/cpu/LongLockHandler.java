/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.internal.NoopMeter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

public final class LongLockHandler extends AbstractThreadDispatchingHandler {
  private static final String EVENT_NAME = "jdk.JavaMonitorWait";
  private static final String METRIC_NAME = "runtime.jvm.cpu.longlock.time";
  private static final String DESCRIPTION = "Long lock times";

  private DoubleHistogram histogram;

  public LongLockHandler(ThreadGrouper grouper) {
    super(grouper);
    initializeMeter(NoopMeter.getInstance());
  }

  @Override
  public void initializeMeter(Meter meter) {
    histogram =
        meter
            .histogramBuilder(METRIC_NAME)
            .setDescription(DESCRIPTION)
            .setUnit(Constants.MILLISECONDS)
            .build();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Consumer<RecordedEvent> createPerThreadSummarizer(String threadName) {
    return new PerThreadLongLockHandler(histogram, threadName);
  }

  @Override
  public Optional<Duration> getThreshold() {
    return Optional.empty();
  }

  private static class PerThreadLongLockHandler implements Consumer<RecordedEvent> {
    private static final String EVENT_THREAD = "eventThread";

    private final BoundDoubleHistogram boundHistogram;

    public PerThreadLongLockHandler(DoubleHistogram histogram, String threadName) {
      this.boundHistogram = histogram.bind(Attributes.of(ATTR_THREAD_NAME, threadName));
    }

    @Override
    public void accept(RecordedEvent recordedEvent) {
      if (recordedEvent.hasField(EVENT_THREAD)) {
        boundHistogram.record(recordedEvent.getDuration().toMillis());
      }
      // What about the class name in MONITOR_CLASS ?
      // We can get a stack trace from the thread on the event
      // var eventThread = recordedEvent.getThread(EVENT_THREAD);
    }
  }
}
