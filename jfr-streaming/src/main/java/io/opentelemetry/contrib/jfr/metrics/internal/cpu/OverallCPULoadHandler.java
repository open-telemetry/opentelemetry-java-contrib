/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_UTILIZATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class OverallCPULoadHandler implements RecordedEventHandler {
  private static final String METRIC_NAME_PROCESS = "process.runtime.jvm.cpu.utilization";
  private static final String METRIC_NAME_MACHINE = "process.runtime.jvm.system.cpu.utilization";
  private static final String METRIC_DESCRIPTION_PROCESS = "Recent CPU utilization for the process";
  private static final String METRIC_DESCRIPTION_MACHINE =
      "Recent CPU utilization for the whole system";

  private static final String EVENT_NAME = "jdk.CPULoad";
  private static final String JVM_USER = "jvmUser";
  private static final String JVM_SYSTEM = "jvmSystem";
  private static final String MACHINE_TOTAL = "machineTotal";
  private volatile double process = 0;
  private volatile double machine = 0;

  public OverallCPULoadHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .gaugeBuilder(METRIC_NAME_PROCESS)
        .setDescription(METRIC_DESCRIPTION_PROCESS)
        .setUnit(UNIT_UTILIZATION)
        .buildWithCallback(
            measurement -> {
              measurement.record(process);
            });
    meter
        .gaugeBuilder(METRIC_NAME_MACHINE)
        .setDescription(METRIC_DESCRIPTION_MACHINE)
        .setUnit(UNIT_UTILIZATION)
        .buildWithCallback(
            measurement -> {
              measurement.record(machine);
            });
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(JVM_USER) && ev.hasField(JVM_SYSTEM)) {
      process = ev.getDouble(JVM_USER) + ev.getDouble(JVM_SYSTEM);
    }
    if (ev.hasField(MACHINE_TOTAL)) {
      machine = ev.getDouble(MACHINE_TOTAL);
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
