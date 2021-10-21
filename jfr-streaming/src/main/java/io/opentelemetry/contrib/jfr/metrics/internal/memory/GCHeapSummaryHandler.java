/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_MEMORY_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.KILOBYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USED;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.HashMap;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/** This class handles GCHeapSummary JFR events. For GC purposes they come in pairs. */
public final class GCHeapSummaryHandler implements RecordedEventHandler {
  private static final String SIMPLE_CLASS_NAME = GCHeapSummaryHandler.class.getSimpleName();
  private static final String METRIC_NAME_DURATION = "runtime.jvm.";
  private static final String METRIC_NAME_MEMORY = "runtime.jvm.memory.utilization";

  //  private static final String JFR_GC_HEAP_SUMMARY_HEAP_USED = "jfr.GCHeapSummary.heapUsed";
  //  private static final String JFR_GC_HEAP_SUMMARY_COMMITTED = "jfr.GCHeapSummary.heapCommitted";
  private static final String EVENT_NAME = "jdk.GCHeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";
  private static final String HEAP_USED = "heapUsed";
  private static final String HEAP_SPACE = "heapSpace";
  private static final String DESCRIPTION = "GC Duration";
  private static final String COMMITTED_SIZE = "committedSize";

  private final Map<Long, RecordedEvent> awaitingPairs = new HashMap<>();

  private final Meter otelMeter;
  private DoubleHistogram durationHistogram;
  private BoundDoubleHistogram usedHistogram;
  private BoundDoubleHistogram committedHistogram;

  public GCHeapSummaryHandler(Meter otelMeter) {
    this.otelMeter = otelMeter;
  }

  public GCHeapSummaryHandler init() {
    var builder = otelMeter.histogramBuilder(METRIC_NAME_DURATION);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(Constants.MILLISECONDS);
    durationHistogram = builder.build();

    var attr = Attributes.of(ATTR_MEMORY_USAGE, USED);
    builder = otelMeter.histogramBuilder(METRIC_NAME_MEMORY);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(KILOBYTES);
    usedHistogram = builder.build().bind(attr);

    attr = Attributes.of(ATTR_MEMORY_USAGE, COMMITTED);
    builder = otelMeter.histogramBuilder(METRIC_NAME_MEMORY);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(KILOBYTES);
    committedHistogram = builder.build().bind(attr);

    return this;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    String when = null;
    if (ev.hasField(WHEN)) {
      when = ev.getString(WHEN);
    }
    if (when != null) {
      if (!(when.equals(BEFORE) || when.equals(AFTER))) {
        return;
      }
    }

    long gcId = 0;
    if (ev.hasField(GC_ID)) {
      gcId = ev.getLong(GC_ID);
    } else {
      return;
    }

    var pair = awaitingPairs.get(gcId);
    if (pair == null) {
      awaitingPairs.put(gcId, ev);
    } else {
      awaitingPairs.remove(gcId);
      if (when != null && when.equals(BEFORE)) {
        recordValues(ev, pair);
      } else { //  i.e. when.equals(AFTER)
        recordValues(pair, ev);
      }
    }
  }

  private void recordValues(RecordedEvent before, RecordedEvent after) {
    durationHistogram.record(
        after.getStartTime().toEpochMilli() - before.getStartTime().toEpochMilli());
    if (after.hasField(HEAP_USED)) {
      usedHistogram.record(after.getLong(HEAP_USED));
    }
    if (after.hasField(HEAP_SPACE)) {
      after.getValue(HEAP_SPACE);
      if (after.getValue(HEAP_SPACE) instanceof RecordedObject) {
        RecordedObject ro = after.getValue(HEAP_SPACE);
        committedHistogram.record(ro.getLong(COMMITTED_SIZE));
      }
    }
  }
}
