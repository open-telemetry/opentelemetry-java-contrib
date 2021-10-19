/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates all TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationInNewTLABHandler implements RecordedEventHandler {

  public static final String HISTOGRAM_NAME = "jfr.ObjectAllocationInNewTLAB.allocation";
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
    var attr = Attributes.of(ATTR_THREAD_NAME, threadName);
    var builder = otelMeter.histogramBuilder(HISTOGRAM_NAME);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(Constants.KILOBYTES);
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
