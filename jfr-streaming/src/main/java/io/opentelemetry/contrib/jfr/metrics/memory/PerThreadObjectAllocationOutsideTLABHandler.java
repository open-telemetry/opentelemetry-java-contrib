/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.memory;

import static io.opentelemetry.contrib.jfr.metrics.Constants.ATTR_THREAD_NAME;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.Constants;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates all non-TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationOutsideTLABHandler implements RecordedEventHandler {
  public static final String JFR_OBJECT_ALLOCATION_OUTSIDE_TLAB_ALLOCATION =
      "jfr.ObjectAllocationOutsideTLAB.allocation";
  public static final String ALLOCATION_SIZE = "allocationSize";
  private static final String DESCRIPTION = "Non-TLAB Allocation";

  private final String threadName;
  private final Meter otelMeter;
  private BoundDoubleHistogram histogram;

  public PerThreadObjectAllocationOutsideTLABHandler(Meter otelMeter, String threadName) {
    this.otelMeter = otelMeter;
    this.threadName = threadName;
  }

  public PerThreadObjectAllocationOutsideTLABHandler init() {
    var attr = Attributes.of(ATTR_THREAD_NAME, threadName);
    var builder = otelMeter.histogramBuilder(JFR_OBJECT_ALLOCATION_OUTSIDE_TLAB_ALLOCATION);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(Constants.KILOBYTES);
    histogram = builder.build().bind(attr);
    return this;
  }

  @Override
  public String getEventName() {
    return ObjectAllocationOutsideTLABHandler.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    histogram.record(ev.getLong(ALLOCATION_SIZE));
    // Probably too high a cardinality
    // ev.getClass("objectClass").getName();
  }
}
