/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NON_HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USED;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/** This class handles GCHeapConfiguration JFR events. */
public final class MetaspaceSummaryHandler implements RecordedEventHandler {
  private static final Logger logger = Logger.getLogger(ParallelHeapSummaryHandler.class.getName());
  private static final String EVENT_NAME = "jdk.MetaspaceSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";
  private static final String GC_ID = "gcId";
  private static final String WHEN = "when";

  private static final Attributes ATTR_MEMORY_METASPACE =
      Attributes.of(ATTR_TYPE, NON_HEAP, ATTR_POOL, "Metaspace");
  private static final Attributes ATTR_MEMORY_COMPRESSED_CLASS_SPACE =
      Attributes.of(ATTR_TYPE, NON_HEAP, ATTR_POOL, "Compressed Class Space");

  private volatile long classUsageBefore = 0;
  private volatile long classUsageAfter = 0;
  private volatile long classCommitted = 0;
  private volatile long totalUsageBefore = 0;
  private volatile long totalUsageAfter = 0;
  private volatile long totalCommitted = 0;

  public MetaspaceSummaryHandler() {
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
              measurement.record(classUsageBefore, ATTR_MEMORY_COMPRESSED_CLASS_SPACE);
              measurement.record(totalUsageBefore, ATTR_MEMORY_METASPACE);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_AFTER)
        .setDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(classUsageAfter, ATTR_MEMORY_COMPRESSED_CLASS_SPACE);
              measurement.record(totalUsageAfter, ATTR_MEMORY_METASPACE);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_COMMITTED)
        .setDescription(METRIC_DESCRIPTION_COMMITTED)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(classCommitted, ATTR_MEMORY_COMPRESSED_CLASS_SPACE);
              measurement.record(totalCommitted, ATTR_MEMORY_METASPACE);
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
      logger.fine(String.format("Metaspace Event seen without when: %s", ev));
      return;
    }
    if (!(BEFORE.equals(when) || AFTER.equals(when))) {
      logger.fine(
          String.format("Metaspace Event seen where when is neither before nor after: %s", ev));
      return;
    }

    if (!ev.hasField(GC_ID)) {
      logger.fine(String.format("Metaspace Event seen without GC ID: %s", ev));
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
        "classSpace",
        classSpace -> {
          if (classSpace.hasField(COMMITTED)) {
            classCommitted = classSpace.getLong(COMMITTED);
          }
          if (classSpace.hasField(USED)) {
            if (before) {
              classUsageBefore = classSpace.getLong(USED);
            } else {
              classUsageAfter = classSpace.getLong(USED);
            }
          }
        });

    doIfAvailable(
        event,
        "metaspace",
        classSpace -> {
          if (classSpace.hasField(COMMITTED)) {
            totalCommitted = classSpace.getLong(COMMITTED);
          }
          if (classSpace.hasField(USED)) {
            if (before) {
              totalUsageBefore = classSpace.getLong(USED);
            } else {
              totalUsageAfter = classSpace.getLong(USED);
            }
          }
        });
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
