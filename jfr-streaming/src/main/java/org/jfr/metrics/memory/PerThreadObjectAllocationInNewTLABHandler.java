package org.jfr.metrics.memory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import jdk.jfr.consumer.RecordedEvent;
import org.jfr.metrics.RecordedEventHandler;

import static org.jfr.metrics.Constants.KILOBYTES;
import static org.jfr.metrics.Constants.THREAD_NAME;

/** This class aggregates all TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationInNewTLABHandler implements RecordedEventHandler {

  public static final String HISTOGRAM_NAME =
          "jfr.ObjectAllocationInNewTLAB.allocation";
  public static final String TLAB_SIZE = "tlabSize";
  private static final String DESCRIPTION = "TLAB Allocation";

  private final String threadName;
  private final Meter otelMeter;
  private BoundDoubleHistogram histogram;

  public PerThreadObjectAllocationInNewTLABHandler(Meter otelMeter, String threadName) {
    this.threadName = threadName;
    this.otelMeter = otelMeter;
  }

  public PerThreadObjectAllocationInNewTLABHandler init() {
    var attr = Attributes.of(AttributeKey.stringKey(THREAD_NAME), threadName);
    var builder = otelMeter.histogramBuilder(HISTOGRAM_NAME);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(KILOBYTES);
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
