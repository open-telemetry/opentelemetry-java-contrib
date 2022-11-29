/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.COMMITTED_SIZE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USED;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.function.Consumer;
import java.util.logging.Logger;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/**
 * This class handles G1HeapSummary JFR events. For GC purposes they come in pairs. Basic heap
 * values are sourced from GCHeapSummary - this is young generational details
 */
public final class ParallelHeapSummaryHandler implements RecordedEventHandler {
  private static final Logger logger = Logger.getLogger(ParallelHeapSummaryHandler.class.getName());
  private static final String EVENT_NAME = "jdk.PSHeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";
  private static final Attributes ATTR_MEMORY_EDEN_USED =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Eden Space");
  private static final Attributes ATTR_MEMORY_SURVIVOR_USED =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Survivor Space");
  private static final Attributes ATTR_MEMORY_OLD_USED =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Old Gen");
  private static final Attributes ATTR_MEMORY_COMMITTED_OLD =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Old Gen");
  private static final Attributes ATTR_MEMORY_COMMITTED_YOUNG =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "PS Eden Space");

  private volatile long usageEden = 0;
  private volatile long usageEdenAfter = 0;
  private volatile long usageSurvivor = 0;
  private volatile long usageSurvivorAfter = 0;
  private volatile long usageOld = 0;
  private volatile long usageOldAfter = 0;
  private volatile long committedOld = 0;
  private volatile long committedYoung = 0;

  public ParallelHeapSummaryHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY)
        .setDescription(METRIC_DESCRIPTION_MEMORY)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(usageEden, ATTR_MEMORY_EDEN_USED);
              measurement.record(usageSurvivor, ATTR_MEMORY_SURVIVOR_USED);
              measurement.record(usageOld, ATTR_MEMORY_OLD_USED);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_AFTER)
        .setDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(usageEdenAfter, ATTR_MEMORY_EDEN_USED);
              measurement.record(usageSurvivorAfter, ATTR_MEMORY_SURVIVOR_USED);
              measurement.record(usageOldAfter, ATTR_MEMORY_OLD_USED);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_COMMITTED)
        .setDescription(METRIC_DESCRIPTION_COMMITTED)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(committedOld, ATTR_MEMORY_COMMITTED_OLD);
              measurement.record(committedYoung, ATTR_MEMORY_COMMITTED_YOUNG);
            });
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
      logger.fine(String.format("Parallel GC Event seen without when: %s", ev));
      return;
    }
    if (!(BEFORE.equals(when) || AFTER.equals(when))) {
      logger.fine(
          String.format("Parallel GC Event seen where when is neither before nor after: %s", ev));
      return;
    }

    if (!ev.hasField(GC_ID)) {
      logger.fine(String.format("Parallel GC Event seen without GC ID: %s", ev));
      return;
    }
    recordValues(ev, BEFORE.equals(when));
  }

  private static void doIfAvailable(
      RecordedEvent event, String field, Consumer<RecordedObject> closure) {
    if (!event.hasField(field)) {
      return;
    }
    Object value = event.getValue(field);
    if (value instanceof RecordedObject) {
      closure.accept((RecordedObject) value);
    }
  }

  private void recordValues(RecordedEvent event, boolean before) {

    doIfAvailable(
        event,
        "edenSpace",
        edenSpace -> {
          if (edenSpace.hasField(USED)) {
            if (before) {
              usageEden = edenSpace.getLong(USED);
            } else {
              usageEdenAfter = edenSpace.getLong(USED);
            }
          }
        });

    doIfAvailable(
        event,
        "fromSpace",
        fromSpace -> {
          if (fromSpace.hasField(USED)) {
            if (before) {
              usageSurvivor = fromSpace.getLong(USED);
            } else {
              usageSurvivorAfter = fromSpace.getLong(USED);
            }
          }
        });

    doIfAvailable(
        event,
        "oldObjectSpace",
        oldObjectSpace -> {
          if (oldObjectSpace.hasField(USED)) {
            if (before) {
              usageSurvivor = oldObjectSpace.getLong(USED);
            } else {
              usageSurvivorAfter = oldObjectSpace.getLong(USED);
            }
          }
        });

    doIfAvailable(
        event,
        "oldSpace",
        oldSpace -> {
          if (oldSpace.hasField(COMMITTED_SIZE)) {
            committedOld = oldSpace.getLong(COMMITTED_SIZE);
          }
        });

    doIfAvailable(
        event,
        "youngSpace",
        youngSpace -> {
          if (youngSpace.hasField(COMMITTED_SIZE)) {
            committedYoung = youngSpace.getLong(COMMITTED_SIZE);
          }
        });
  }
}
