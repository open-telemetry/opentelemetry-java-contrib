/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

public final class PerThreadLongLockHandler implements RecordedEventHandler {
  private static final String SIMPLE_CLASS_NAME = PerThreadLongLockHandler.class.getSimpleName();
  private static final String MONITOR_CLASS = "monitorClass";
  private static final String CLASS = "class";
  private static final String EVENT_THREAD = "eventThread";
  private static final String DURATION = "duration";
  private static final String STACK_TRACE = "stackTrace";
  private static final String JFR_JAVA_MONITOR_WAIT = "JfrJavaMonitorWait";

  private final String threadName;
  private final BoundDoubleHistogram histogram;

  public PerThreadLongLockHandler(BoundDoubleHistogram histogram, String threadName) {
    this.threadName = threadName;
    this.histogram = histogram;
  }

  @Override
  public String getEventName() {
    return LongLockHandler.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent recordedEvent) {
    if (recordedEvent.hasField(EVENT_THREAD)) {
      histogram.record(recordedEvent.getDuration().toMillis());
    }
    // What about the class name in MONITOR_CLASS ?
    // We can get a stack trace from the thread on the event
    // var eventThread = recordedEvent.getThread(EVENT_THREAD);
  }
}
