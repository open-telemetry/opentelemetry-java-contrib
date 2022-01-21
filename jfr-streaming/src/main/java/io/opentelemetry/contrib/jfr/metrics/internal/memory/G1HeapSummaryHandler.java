/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.EDEN_SIZE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.EDEN_SIZE_DELTA;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.EDEN_USED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.REGION_COUNT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.SURVIVOR_SIZE;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jdk.jfr.consumer.RecordedEvent;

/**
 * This class handles G1HeapSummary JFR events. For GC purposes they come in pairs. Basic heap
 * values are sourced from GCHeapSummary - this is young generational details
 */
public final class G1HeapSummaryHandler implements RecordedEventHandler {
  private static final Logger logger = Logger.getLogger(G1HeapSummaryHandler.class.getName());

  private static final String METRIC_NAME_MEMORY = "process.runtime.jvm.memory.used";
  private static final String METRIC_DESCRIPTION_MEMORY = "Heap utilization";
  private static final String EVENT_NAME = "jdk.G1HeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";
  private static final Attributes ATTR_MEMORY_EDEN_USED = Attributes.of(ATTR_USAGE, EDEN_USED);
  private static final Attributes ATTR_MEMORY_EDEN_SIZE = Attributes.of(ATTR_USAGE, EDEN_SIZE);
  private static final Attributes ATTR_MEMORY_EDEN_SIZE_DELTA =
      Attributes.of(ATTR_USAGE, EDEN_SIZE_DELTA);
  private static final Attributes ATTR_MEMORY_SURVIVOR_SIZE =
      Attributes.of(ATTR_USAGE, SURVIVOR_SIZE);
  private static final Attributes ATTR_MEMORY_REGIONS = Attributes.of(ATTR_USAGE, REGION_COUNT);

  private final Map<Long, RecordedEvent> awaitingPairs = new HashMap<>();

  private DoubleHistogram memoryHistogram;

  public G1HeapSummaryHandler() {
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
    String when;
    if (ev.hasField(WHEN)) {
      when = ev.getString(WHEN);
    } else {
      logger.fine(String.format("G1 GC Event seen without when: %s", ev));
      return;
    }
    if (!(when.equals(BEFORE) || when.equals(AFTER))) {
      logger.fine(String.format("G1 GC Event seen where when is neither before nor after: %s", ev));
      return;
    }

    if (!ev.hasField(GC_ID)) {
      return;
    }
    long gcId = ev.getLong(GC_ID);

    var pair = awaitingPairs.remove(gcId);
    if (pair == null) {
      awaitingPairs.put(gcId, ev);
    } else {
      if (when.equals(BEFORE)) {
        recordValues(ev, pair);
      } else { //  i.e. when.equals(AFTER)
        recordValues(pair, ev);
      }
    }
  }

  private void recordValues(RecordedEvent before, RecordedEvent after) {
    if (after.hasField("edenUsedSize")) {
      memoryHistogram.record(after.getLong("edenUsedSize"), ATTR_MEMORY_EDEN_USED);
      if (before.hasField("edenUsedSize")) {
        memoryHistogram.record(
            after.getLong("edenUsedSize") - before.getLong("edenUsedSize"),
            ATTR_MEMORY_EDEN_SIZE_DELTA);
      }
    }
    if (after.hasField("edenTotalSize")) {
      memoryHistogram.record(after.getLong("edenTotalSize"), ATTR_MEMORY_EDEN_SIZE);
    }
    if (after.hasField("survivorUsedSize")) {
      memoryHistogram.record(after.getLong("survivorUsedSize"), ATTR_MEMORY_SURVIVOR_SIZE);
    }
    if (after.hasField("numberOfRegions")) {
      memoryHistogram.record(after.getLong("numberOfRegions"), ATTR_MEMORY_REGIONS);
    }
  }
}
