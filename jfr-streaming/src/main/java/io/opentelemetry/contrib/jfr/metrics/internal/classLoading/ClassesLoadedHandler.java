/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.classLoading;

import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class ClassesLoadedHandler implements RecordedEventHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.cpu.loaded_class_count";
  private static final String EVENT_NAME = "jdk.ClassLoadingStatistics";
  private static final String METRIC_DESCRIPTION = "Number of loaded classes";

  private volatile long value = 0;

  public ClassesLoadedHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void accept(RecordedEvent ev) {
    value = ev.getLong("loadedClassCount");
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .gaugeBuilder(METRIC_NAME)
        .ofLongs()
        .setDescription(METRIC_DESCRIPTION)
        .buildWithCallback(measurement -> measurement.record(value));
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
