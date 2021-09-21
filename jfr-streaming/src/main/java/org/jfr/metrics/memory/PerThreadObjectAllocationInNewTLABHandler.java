package org.jfr.metrics.memory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import jdk.jfr.consumer.RecordedEvent;
import org.jfr.metrics.RecordedEventHandler;

/** This class aggregates all TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationInNewTLABHandler implements RecordedEventHandler {

  public static final String JFR_OBJECT_ALLOCATION_IN_NEW_TLAB_ALLOCATION =
          "jfr.ObjectAllocationInNewTLAB.allocation";
  public static final String THREAD_NAME = "thread.name";
  public static final String TLAB_SIZE = "tlabSize";

  private final String threadName;
  private final Meter otelMeter;
  private BoundDoubleHistogram histogram;

  public PerThreadObjectAllocationInNewTLABHandler(Meter otelMeter, String threadName) {
    this.threadName = threadName;
    this.otelMeter = otelMeter;
  }

  public PerThreadObjectAllocationInNewTLABHandler init() {
    var attr = Attributes.of(AttributeKey.stringKey(THREAD_NAME), threadName);
    var builder = otelMeter.histogramBuilder(JFR_OBJECT_ALLOCATION_IN_NEW_TLAB_ALLOCATION);
    builder.setDescription("TLAB Allocation");
    builder.setUnit("KB");
    histogram = builder.build().bind(attr);
    return this;
  }

  @Override
  public String getEventName() {
    return ObjectAllocationInNewTLABHandler.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    histogram.record(ev.getLong(TLAB_SIZE));
    // Probably too high a cardinality
    // ev.getClass("objectClass").getName();
  }
}
