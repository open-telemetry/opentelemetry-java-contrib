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
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/** This class handles GCHeapSummary JFR events. For GC purposes they come in pairs. */
public final class GCHeapSummaryHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.GCHeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";
  private static final String HEAP_USED = "heapUsed";
  private static final String HEAP_SPACE = "heapSpace";
  private static final Attributes ATTR =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "Java heap space");

  private volatile long usageBefore = 0;
  private volatile long usageAfter = 0;
  private volatile long committedSize = 0;

  public GCHeapSummaryHandler() {
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
              measurement.record(usageBefore, ATTR);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_AFTER)
        .setDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(usageAfter, ATTR);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_COMMITTED)
        .setDescription(METRIC_DESCRIPTION_COMMITTED)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(committedSize, ATTR);
            });
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
    recordValues(ev, BEFORE.equals(when));
  }

  private void recordValues(RecordedEvent event, boolean before) {

    if (event.hasField(HEAP_USED)) {
      if (before) {
        usageBefore = event.getLong(HEAP_USED);
      } else {
        usageAfter = event.getLong(HEAP_USED);
      }
    }
    if (event.hasField(HEAP_SPACE)) {
      Object heapSpaceValue = event.getValue(HEAP_SPACE);
      if (heapSpaceValue instanceof RecordedObject) {
        RecordedObject heapSpace = (RecordedObject) heapSpaceValue;
        if (heapSpace.hasField(COMMITTED_SIZE)) {
          committedSize = heapSpace.getLong(COMMITTED_SIZE);
        }
      }
    }
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
