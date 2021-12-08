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
import io.opentelemetry.api.metrics.internal.NoopMeter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

/**
 * This class handles TLAB allocation JFR events, and delegates them to the actual per-thread
 * aggregators
 */
public final class ObjectAllocationInNewTLABHandler extends AbstractThreadDispatchingHandler {
  private static final String EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  private static final String DESCRIPTION = "Allocation";

  private DoubleHistogram histogram;

  public ObjectAllocationInNewTLABHandler(ThreadGrouper grouper) {
    super(grouper);
    initializeMeter(NoopMeter.getInstance());
  }

  @Override
  public void initializeMeter(Meter meter) {
    histogram =
        meter
            .histogramBuilder(METRIC_NAME_MEMORY_ALLOCATION)
            .setDescription(DESCRIPTION)
            .setUnit(BYTES)
            .build();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Consumer<RecordedEvent> createPerThreadSummarizer(String threadName) {
    return new PerThreadObjectAllocationInNewTLABHandler(histogram, threadName);
  }

  /** This class aggregates all TLAB allocation JFR events for a single thread */
  private static class PerThreadObjectAllocationInNewTLABHandler
      implements Consumer<RecordedEvent> {
    private static final String TLAB_SIZE = "tlabSize";
    private static final String TLAB = "TLAB";

    private final BoundDoubleHistogram boundHistogram;

    public PerThreadObjectAllocationInNewTLABHandler(DoubleHistogram histogram, String threadName) {
      boundHistogram =
          histogram.bind(Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_ARENA_NAME, TLAB));
    }

    @Override
    public void accept(RecordedEvent ev) {
      boundHistogram.record(ev.getLong(TLAB_SIZE));
      // Probably too high a cardinality
      // ev.getClass("objectClass").getName();
    }
  }
}
