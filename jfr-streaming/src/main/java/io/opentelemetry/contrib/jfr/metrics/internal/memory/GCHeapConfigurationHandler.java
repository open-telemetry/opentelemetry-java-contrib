/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_POOL;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HEAP;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.INITIAL_SIZE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_INIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_MEMORY_LIMIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_INIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_MEMORY_LIMIT;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

/** This class handles GCHeapConfiguration JFR events. */
public final class GCHeapConfigurationHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.GCHeapConfiguration";

  private static final String MAX_SIZE = "maxSize";
  private static final Attributes ATTR =
      Attributes.of(ATTR_TYPE, HEAP, ATTR_POOL, "Java heap space");

  private volatile long init = 0;
  private volatile long limit = 0;

  public GCHeapConfigurationHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_INIT)
        .setDescription(METRIC_DESCRIPTION_MEMORY_INIT)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(init, ATTR);
            });
    meter
        .upDownCounterBuilder(METRIC_NAME_MEMORY_LIMIT)
        .setDescription(METRIC_DESCRIPTION_MEMORY_LIMIT)
        .setUnit(BYTES)
        .buildWithCallback(
            measurement -> {
              measurement.record(limit, ATTR);
            });
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent event) {
    if (event.hasField(INITIAL_SIZE)) {
      init = event.getLong(INITIAL_SIZE);
    }
    if (event.hasField(MAX_SIZE)) {
      limit = event.getLong(MAX_SIZE);
    }
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
