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
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_LIMIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_COMMITTED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_LIMIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NON_HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.RESERVED;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USED;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

/** This class handles GCHeapConfiguration JFR events. */
public final class MetaspaceSummaryHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.MetaspaceSummary";

  private static final Attributes ATTR_MEMORY_METASPACE =
      Attributes.of(ATTR_TYPE, NON_HEAP, ATTR_POOL, "Metaspace");
  private static final Attributes ATTR_MEMORY_COMPRESSED_CLASS_SPACE =
      Attributes.of(ATTR_TYPE, NON_HEAP, ATTR_POOL, "Compressed Class Space");

  private volatile long classUsage = 0;
  private volatile long classCommitted = 0;
  private volatile long totalUsage = 0;
  private volatile long totalCommitted = 0;
  private volatile long classLimit = 0;
  private volatile long totalLimit = 0;

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
              measurement.record(classUsage, ATTR_MEMORY_COMPRESSED_CLASS_SPACE);
              measurement.record(totalUsage, ATTR_MEMORY_METASPACE);
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
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_LIMIT)
        .setDescription(METRIC_DESCRIPTION_MEMORY_LIMIT)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(classLimit, ATTR_MEMORY_COMPRESSED_CLASS_SPACE);
              measurement.record(totalLimit, ATTR_MEMORY_METASPACE);
            });
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent event) {
    doIfAvailable(
        event,
        "classSpace",
        classSpace -> {
          if (classSpace.hasField(COMMITTED)) {
            classCommitted = classSpace.getLong(COMMITTED);
          }
          if (classSpace.hasField(USED)) {
            classUsage = classSpace.getLong(USED);
          }
          if (classSpace.hasField(RESERVED)) {
            classLimit = classSpace.getLong(RESERVED);
          }
        });

    doIfAvailable(
        event,
        "metaspace",
        metaspace -> {
          if (metaspace.hasField(COMMITTED)) {
            totalCommitted = metaspace.getLong(COMMITTED);
          }
          if (metaspace.hasField(USED)) {
            totalUsage = metaspace.getLong(USED);
          }
          if (metaspace.hasField(RESERVED)) {
            totalLimit = metaspace.getLong(RESERVED);
          }
        });
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

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
