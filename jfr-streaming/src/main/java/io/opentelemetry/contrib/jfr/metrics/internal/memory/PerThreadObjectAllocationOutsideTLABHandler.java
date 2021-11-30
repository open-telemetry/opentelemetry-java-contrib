/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_ARENA_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_ALLOCATION;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates all non-TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationOutsideTLABHandler implements RecordedEventHandler {
  private static final String ALLOCATION_SIZE = "allocationSize";
  private static final String DESCRIPTION = "Allocation";
  private static final String MAIN = "Main";

  private final String threadName;
  private final Meter otelMeter;
  private BoundDoubleHistogram histogram;

  public PerThreadObjectAllocationOutsideTLABHandler(Meter otelMeter, String threadName) {
    this.otelMeter = otelMeter;
    this.threadName = threadName;
  }

  public PerThreadObjectAllocationOutsideTLABHandler init() {
    var attr = Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_ARENA_NAME, MAIN);
    var builder = otelMeter.histogramBuilder(METRIC_NAME_MEMORY_ALLOCATION);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(BYTES);
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
