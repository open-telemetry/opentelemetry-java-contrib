/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

public final class LongLockHandler extends AbstractThreadDispatchingHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.cpu.longlock";
  private static final String METRIC_DESCRIPTION = "Long lock times";
  private static final String EVENT_NAME = "jdk.JavaMonitorWait";

  private DoubleHistogram histogram;

  public LongLockHandler(ThreadGrouper grouper) {
    super(grouper);
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    histogram =
        meter
            .histogramBuilder(METRIC_NAME)
            .setDescription(METRIC_DESCRIPTION)
            .setUnit(MILLISECONDS)
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

    private final DoubleHistogram histogram;
    private final Attributes attributes;

    public PerThreadLongLockHandler(DoubleHistogram histogram, String threadName) {
      this.histogram = histogram;
      this.attributes = Attributes.of(ATTR_THREAD_NAME, threadName);
    }

    @Override
    public void accept(RecordedEvent recordedEvent) {
      if (recordedEvent.hasField(EVENT_THREAD)) {
        histogram.record(recordedEvent.getDuration().toMillis(), attributes);
      }
      // What about the class name in MONITOR_CLASS ?
      // We can get a stack trace from the thread on the event
      // var eventThread = recordedEvent.getThread(EVENT_THREAD);
    }
  }
}
