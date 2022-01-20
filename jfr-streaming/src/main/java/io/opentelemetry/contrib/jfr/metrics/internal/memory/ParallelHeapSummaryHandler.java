/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.EDEN_SIZE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.EDEN_SIZE_DELTA;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.SURVIVOR_SIZE;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.HashMap;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/**
 * This class handles G1HeapSummary JFR events. For GC purposes they come in pairs. Basic heap
 * values are sourced from GCHeapSummary - this is young generational details
 */
public final class ParallelHeapSummaryHandler implements RecordedEventHandler {
  private static final String METRIC_NAME_MEMORY = "process.runtime.jvm.memory.used";
  private static final String METRIC_DESCRIPTION_MEMORY = "Heap utilization";
  private static final String EVENT_NAME = "jdk.PSHeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";
  private static final Attributes ATTR_MEMORY_EDEN_SIZE = Attributes.of(ATTR_USAGE, EDEN_SIZE);
  private static final Attributes ATTR_MEMORY_EDEN_SIZE_DELTA =
      Attributes.of(ATTR_USAGE, EDEN_SIZE_DELTA);
  private static final Attributes ATTR_MEMORY_SURVIVOR_SIZE =
      Attributes.of(ATTR_USAGE, SURVIVOR_SIZE);

  private final Map<Long, RecordedEvent> awaitingPairs = new HashMap<>();

  private DoubleHistogram memoryHistogram;

  public ParallelHeapSummaryHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
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
    if (after.hasField("edenSpace") && after.getValue("edenSpace") instanceof RecordedObject edenSpace) {
      memoryHistogram.record(edenSpace.getLong("size"), ATTR_MEMORY_EDEN_SIZE);
      if (before.hasField("edenSpace") && before.getValue("edenSpace") instanceof RecordedObject) {
        RecordedObject beforeSpace = before.getValue("edenSpace");
        memoryHistogram.record(
            edenSpace.getLong("size") - beforeSpace.getLong("size"), ATTR_MEMORY_EDEN_SIZE_DELTA);
      }
    }
    if (after.hasField("fromSpace") && after.getValue("fromSpace") instanceof RecordedObject fromSpace) {
      memoryHistogram.record(fromSpace.getLong("size"), ATTR_MEMORY_SURVIVOR_SIZE);
    }
  }
}
