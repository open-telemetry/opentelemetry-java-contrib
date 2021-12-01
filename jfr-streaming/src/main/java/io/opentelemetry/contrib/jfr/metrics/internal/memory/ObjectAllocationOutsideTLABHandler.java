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
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

/**
 * This class handles all non-TLAB allocation JFR events, and delegates them to the actual
 * per-thread aggregators
 */
public final class ObjectAllocationOutsideTLABHandler extends AbstractThreadDispatchingHandler {
  private static final String EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  private static final String DESCRIPTION = "Allocation";
  private final DoubleHistogram histogram;

  public ObjectAllocationOutsideTLABHandler(Meter otelMeter, ThreadGrouper grouper) {
    super(grouper);
    histogram =
        otelMeter
            .histogramBuilder(METRIC_NAME_MEMORY_ALLOCATION)
            .setDescription(DESCRIPTION)
            .setUnit(BYTES)
            .build();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  public Consumer<RecordedEvent> createPerThreadSummarizer(String threadName) {
    return new PerThreadObjectAllocationOutsideTLABHandler(histogram, threadName);
  }

  /** This class aggregates all non-TLAB allocation JFR events for a single thread */
  private static class PerThreadObjectAllocationOutsideTLABHandler
      implements Consumer<RecordedEvent> {
    private static final String ALLOCATION_SIZE = "allocationSize";
    private static final String MAIN = "Main";

    private final BoundDoubleHistogram boundHistogram;

    public PerThreadObjectAllocationOutsideTLABHandler(
        DoubleHistogram histogram, String threadName) {
      boundHistogram =
          histogram.bind(Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_ARENA_NAME, MAIN));
    }

    @Override
    public void accept(RecordedEvent ev) {
      boundHistogram.record(ev.getLong(ALLOCATION_SIZE));
      // Probably too high a cardinality
      // ev.getClass("objectClass").getName();
    }
  }
}
