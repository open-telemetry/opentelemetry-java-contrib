package org.jfr.metrics.memory;

import io.opentelemetry.api.metrics.Meter;
import org.jfr.metrics.AbstractThreadDispatchingHandler;
import org.jfr.metrics.RecordedEventHandler;
import org.jfr.metrics.ThreadGrouper;

/**
 * This class handles TLAB allocation JFR events, and delegates them to the actual per-thread aggregators
 */
public final class ObjectAllocationInNewTLABHandler extends AbstractThreadDispatchingHandler {

  public static final String EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  private final Meter otelMeter;

  public ObjectAllocationInNewTLABHandler(Meter otelMeter, ThreadGrouper grouper) {
    super(grouper);
    this.otelMeter = otelMeter;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public RecordedEventHandler createPerThreadSummarizer(String threadName) {
    var ret = new PerThreadObjectAllocationInNewTLABHandler(otelMeter, threadName);
    return ret.init();
  }
}
