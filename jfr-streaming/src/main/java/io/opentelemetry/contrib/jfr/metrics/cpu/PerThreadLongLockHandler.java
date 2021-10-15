package io.opentelemetry.contrib.jfr.metrics.cpu;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.Constants;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

public class PerThreadLongLockHandler implements RecordedEventHandler {
  public static final String SIMPLE_CLASS_NAME = PerThreadLongLockHandler.class.getSimpleName();
  public static final String MONITOR_CLASS = "monitorClass";
  public static final String CLASS = "class";
  public static final String THREAD_NAME = "thread.name";
  public static final String EVENT_THREAD = "eventThread";
  public static final String DURATION = "duration";
  public static final String STACK_TRACE = "stackTrace";
  public static final String JFR_JAVA_MONITOR_WAIT = "JfrJavaMonitorWait";

  public static final String HISTOGRAM_NAME = "jfr.JavaMonitorWait.locktime";
  private static final String DESCRIPTION = "Long lock times";

  private final String threadName;
  private final Meter otelMeter;

  private BoundDoubleHistogram histogram;

  public PerThreadLongLockHandler(Meter otelMeter, String threadName) {
    this.threadName = threadName;
    this.otelMeter = otelMeter;
  }

  @Override
  public String getEventName() {
    return LongLockHandler.EVENT_NAME;
  }

  @Override
  public RecordedEventHandler init() {
    var attr = Attributes.of(AttributeKey.stringKey(Constants.THREAD_NAME), threadName);
    var builder = otelMeter.histogramBuilder(HISTOGRAM_NAME);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(Constants.MILLISECONDS);
    histogram = builder.build().bind(attr);
    return this;
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
