/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.AVERAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.COUNT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MAX;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.RESERVED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.TOTAL_USED;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/** This class handles GCHeapSummary JFR events. For GC purposes they come in pairs. */
public final class GCHeapSummaryHandler implements RecordedEventHandler {
  private static final String METRIC_NAME_DURATION = "process.runtime.jvm.gc.time";
  private static final String METRIC_DESCRIPTION_DURATION = "GC Duration";
  private static final String METRIC_NAME_MEMORY = "process.runtime.jvm.memory.used";
  private static final String METRIC_DESCRIPTION_MEMORY = "Heap utilization";
  private static final String EVENT_NAME = "jdk.GCHeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";
  private static final String HEAP_USED = "heapUsed";
  private static final String HEAP_SPACE = "heapSpace";
  private static final String COMMITTED_SIZE = "committedSize";
  private static final String RESERVED_SIZE = "reservedSize";
  private static final Attributes ATTR_MEMORY_USED = Attributes.of(ATTR_USAGE, TOTAL_USED);
  private static final Attributes ATTR_MEMORY_COMMITTED = Attributes.of(ATTR_USAGE, COMMITTED);
  private static final Attributes ATTR_MEMORY_RESERVED = Attributes.of(ATTR_USAGE, RESERVED);
  private static final Attributes ATTR_DURATION_AVERAGE = Attributes.of(ATTR_TYPE, AVERAGE);
  private static final Attributes ATTR_DURATION_COUNT = Attributes.of(ATTR_TYPE, COUNT);
  private static final Attributes ATTR_DURATION_MAX = Attributes.of(ATTR_TYPE, MAX);

  private final Map<Long, RecordedEvent> awaitingPairs = new HashMap<>();

  private DoubleHistogram memoryHistogram;
  private final List<Double> gcDurations = new ArrayList<>();

  public GCHeapSummaryHandler() {
    initializeMeter(defaultMeter());
  }

  private static DoubleSummaryStatistics summarize(List<Double> l) {
    return l.stream().mapToDouble(x -> x).summaryStatistics();
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME_DURATION)
        .ofDoubles()
        .setUnit(MILLISECONDS)
        .setDescription(METRIC_DESCRIPTION_DURATION)
        .buildWithCallback(
            codm -> {
              var summary = summarize(gcDurations);
              codm.record(summary.getAverage(), ATTR_DURATION_AVERAGE);
              codm.record(summary.getCount(), ATTR_DURATION_COUNT);
              codm.record(summary.getMax(), ATTR_DURATION_MAX);
              gcDurations.clear();
            });
    memoryHistogram =
        meter
            .histogramBuilder(METRIC_NAME_MEMORY)
            .setDescription(METRIC_DESCRIPTION_MEMORY)
            .setUnit(BYTES)
            .build();
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

    if (!ev.hasField(GC_ID)) {
      return;
    }
    long gcId = ev.getLong(GC_ID);

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
    gcDurations.add(
        (double) (after.getStartTime().toEpochMilli() - before.getStartTime().toEpochMilli()));
    if (after.hasField(HEAP_USED)) {
      memoryHistogram.record(after.getLong(HEAP_USED), ATTR_MEMORY_USED);
    }
    if (after.hasField(HEAP_SPACE)) {
      if (after.getValue(HEAP_SPACE) instanceof RecordedObject heapSpace) {
        if (heapSpace.hasField(COMMITTED_SIZE)) {
          memoryHistogram.record(heapSpace.getLong(COMMITTED_SIZE), ATTR_MEMORY_COMMITTED);
        }
        if (heapSpace.hasField(RESERVED_SIZE)) {
          memoryHistogram.record(heapSpace.getLong(RESERVED_SIZE), ATTR_MEMORY_RESERVED);
        }
      }
    }
  }
}
