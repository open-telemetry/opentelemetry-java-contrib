package io.opentelemetry.contrib.jfr.metrics.cpu;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.Constants;
import io.opentelemetry.contrib.jfr.metrics.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import java.time.Duration;
import java.util.Collections;
import io.opentelemetry.contrib.jfr.metrics.ThreadGrouper;
import io.opentelemetry.contrib.jfr.metrics.memory.PerThreadObjectAllocationInNewTLABHandler;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public class LongLockHandler extends AbstractThreadDispatchingHandler {

  public static final String EVENT_NAME = "jdk.JavaMonitorWait";
  private final Meter otelMeter;

  public LongLockHandler(Meter otelMeter, ThreadGrouper grouper) {
    super(grouper);
    this.otelMeter = otelMeter;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public RecordedEventHandler createPerThreadSummarizer(String threadName) {
    var ret = new PerThreadLongLockHandler(otelMeter, threadName);
    return ret.init();
  }
}
