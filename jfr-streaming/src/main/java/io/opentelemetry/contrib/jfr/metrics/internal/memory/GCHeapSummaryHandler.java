/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.AVERAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/** This class handles GCHeapSummary JFR events. For GC purposes they come in pairs. */
public final class GCHeapSummaryHandler implements RecordedEventHandler {
  // The OTel 1.0 metrics follow
  private static final String METRIC_NAME_MEMORY_USAGE = "process.runtime.jvm.memory.usage";
  // Missing: process.runtime.jvm.memory.init
  private static final String METRIC_NAME_MEMORY_COMMITTED = "process.runtime.jvm.memory.committed";
  private static final String METRIC_NAME_MEMORY_MAX = "process.runtime.jvm.memory.limit";

  // Experimental GC metrics follow
  //  private static final String METRIC_NAME_DURATION = "process.runtime.jvm.gc.time.stopped";
  //  private static final String METRIC_DESCRIPTION_DURATION = "GC Duration";

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
  private static final Attributes ATTR_DURATION_AVERAGE = Attributes.of(ATTR_TYPE, AVERAGE);
  //  private static final Attributes ATTR_DURATION_COUNT = Attributes.of(ATTR_TYPE, COUNT);
  //  private static final Attributes ATTR_DURATION_MAX = Attributes.of(ATTR_TYPE, MAX);

  private static final Logger logger = Logger.getLogger(GCHeapSummaryHandler.class.getName());

  private final Map<Long, RecordedEvent> awaitingPairs = new HashMap<>();

  private final List<Long> durations = new ArrayList<>();
  private final List<Long> usage = new ArrayList<>();
  private final List<Long> committed = new ArrayList<>();
  private final List<Long> max = new ArrayList<>();

  public GCHeapSummaryHandler() {
    initializeMeter(defaultMeter());
  }

  private static DoubleSummaryStatistics summarize(List<Long> l) {
    return l.stream().mapToDouble(x -> x).summaryStatistics();
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_USAGE)
        .ofDoubles()
        .setUnit(BYTES)
        .setDescription(METRIC_DESCRIPTION_MEMORY)
        .buildWithCallback(
            codm -> {
              var summary = summarize(usage);
              codm.record(summary.getAverage(), ATTR_DURATION_AVERAGE);
              usage.clear();
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_COMMITTED)
        .ofDoubles()
        .setUnit(BYTES)
        .setDescription(METRIC_DESCRIPTION_MEMORY)
        .buildWithCallback(
            codm -> {
              var summary = summarize(committed);
              codm.record(summary.getAverage(), ATTR_DURATION_AVERAGE);
              committed.clear();
            });

    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_MAX)
        .ofDoubles()
        .setUnit(BYTES)
        .setDescription(METRIC_DESCRIPTION_MEMORY)
        .buildWithCallback(
            codm -> {
              var summary = summarize(max);
              codm.record(summary.getAverage(), ATTR_DURATION_AVERAGE);
              max.clear();
            });

    // FIXME
    //    meter
    //        .upDownCounterBuilder(METRIC_NAME_DURATION)
    //        .ofDoubles()
    //        .setUnit(MILLISECONDS)
    //        .setDescription(METRIC_DESCRIPTION_DURATION)
    //        .buildWithCallback(
    //            codm -> {
    //              var summary = summarize(durations);
    //              codm.record(summary.getAverage(), ATTR_DURATION_AVERAGE);
    //              codm.record(summary.getCount(), ATTR_DURATION_COUNT);
    //              codm.record(summary.getMax(), ATTR_DURATION_MAX);
    //              durations.clear();
    //            });

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
    if (!(BEFORE.equals(when) || AFTER.equals(when))) {
      logger.fine(String.format("GC Event seen with strange " + WHEN + " %s", ev));
      return;
    }

    if (!ev.hasField(GC_ID)) {
      logger.fine(String.format("GC Event seen without " + GC_ID + " %s", ev));
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
    durations.add(after.getStartTime().toEpochMilli() - before.getStartTime().toEpochMilli());

    if (!after.hasField(HEAP_USED)) {
      logger.fine(String.format("GC Event seen without " + HEAP_USED + " %s", after));
      return;
    }
    usage.add(after.getLong(HEAP_USED));

    if (!after.hasField(HEAP_SPACE)) {
      logger.fine(String.format("GC Event seen without " + HEAP_SPACE + " %s", after));
      return;
    }
    if (after.getValue(HEAP_SPACE) instanceof RecordedObject heapSpace) {
      if (!heapSpace.hasField(COMMITTED_SIZE)) {
        logger.fine(String.format("GC Event seen without " + COMMITTED_SIZE + " %s", after));
        return;
      }
      committed.add(heapSpace.getLong(COMMITTED_SIZE));
      if (!heapSpace.hasField(RESERVED_SIZE)) {
        logger.fine(String.format("GC Event seen without " + RESERVED_SIZE + " %s", after));
        return;
      }
      max.add(heapSpace.getLong(RESERVED_SIZE));
    }
  }
}
