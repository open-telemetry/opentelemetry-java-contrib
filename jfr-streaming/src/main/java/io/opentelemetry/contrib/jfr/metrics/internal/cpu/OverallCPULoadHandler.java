/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MACHINE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.PERCENTAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.SYSTEM;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USER;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class OverallCPULoadHandler implements RecordedEventHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.cpu.used";
  private static final String METRIC_DESCRIPTION = "CPU Utilization";
  private static final String EVENT_NAME = "jdk.CPULoad";
  private static final String JVM_USER = "jvmUser";
  private static final String JVM_SYSTEM = "jvmSystem";
  private static final String MACHINE_TOTAL = "machineTotal";

  private static final Attributes ATTR_USER = Attributes.of(ATTR_USAGE, USER);
  private static final Attributes ATTR_SYSTEM = Attributes.of(ATTR_USAGE, SYSTEM);
  private static final Attributes ATTR_MACHINE = Attributes.of(ATTR_USAGE, MACHINE);

  private DoubleHistogram histogram;

  public OverallCPULoadHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    histogram =
        meter
            .histogramBuilder(METRIC_NAME)
            .setDescription(METRIC_DESCRIPTION)
            .setUnit(PERCENTAGE)
            .build();
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(JVM_USER)) {
      histogram.record(ev.getDouble(JVM_USER), ATTR_USER);
    }
    if (ev.hasField(JVM_SYSTEM)) {
      histogram.record(ev.getDouble(JVM_SYSTEM), ATTR_SYSTEM);
    }
    if (ev.hasField(MACHINE_TOTAL)) {
      histogram.record(ev.getDouble(MACHINE_TOTAL), ATTR_MACHINE);
    }
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
