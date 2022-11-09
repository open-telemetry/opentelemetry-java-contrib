/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.classes;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_CLASSES;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class ClassesLoadedHandler implements RecordedEventHandler {
  private static final String METRIC_NAME_LOADED = "process.runtime.jvm.classes.loaded";
  private static final String METRIC_NAME_UNLOADED = "process.runtime.jvm.classes.unloaded";
  private static final String METRIC_NAME_CURRENT = "process.runtime.jvm.classes.current_loaded";
  private static final String EVENT_NAME = "jdk.ClassLoadingStatistics";
  private static final String METRIC_DESCRIPTION_CURRENT = "Number of classes currently loaded";
  private static final String METRIC_DESCRIPTION_LOADED =
      "Number of classes loaded since JVM start";
  private static final String METRIC_DESCRIPTION_UNLOADED =
      "Number of classes unloaded since JVM start";
  private volatile long loaded = 0;
  private volatile long unloaded = 0;

  public ClassesLoadedHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void accept(RecordedEvent ev) {
    loaded = ev.getLong("loadedClassCount");
    unloaded = ev.getLong("unloadedClassCount");
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME_CURRENT)
        .setDescription(METRIC_DESCRIPTION_CURRENT)
        .setUnit(UNIT_CLASSES)
        .buildWithCallback(measurement -> measurement.record(loaded - unloaded));
    meter
        .counterBuilder(METRIC_NAME_LOADED)
        .setDescription(METRIC_DESCRIPTION_LOADED)
        .setUnit(UNIT_CLASSES)
        .buildWithCallback(measurement -> measurement.record(loaded));
    meter
        .counterBuilder(METRIC_NAME_UNLOADED)
        .setDescription(METRIC_DESCRIPTION_UNLOADED)
        .setUnit(UNIT_CLASSES)
        .buildWithCallback(measurement -> measurement.record(unloaded));
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
