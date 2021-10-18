/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.cpu;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.Constants;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public class OverallCPULoadHandler implements RecordedEventHandler {
  public static final String SIMPLE_CLASS_NAME = OverallCPULoadHandler.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.CPULoad";
  public static final String JVM_USER = "jvmUser";
  public static final String JVM_SYSTEM = "jvmSystem";
  public static final String MACHINE_TOTAL = "machineTotal";
  public static final String JFR_CPU_LOAD_JVM_USER = "jfr.CPULoad.jvmUser";
  public static final String JFR_CPU_LOAD_JVM_SYSTEM = "jfr.CPULoad.jvmSystem";
  public static final String JFR_CPU_LOAD_MACHINE_TOTAL = "jfr.CPULoad.machineTotal";

  private final Meter otelMeter;

  private volatile double userValue = 0;
  private volatile double systemValue = 0;
  private volatile double machineTotal = 0;

  public OverallCPULoadHandler(Meter otelMeter) {
    this.otelMeter = otelMeter;
  }

  public OverallCPULoadHandler init() {
    otelMeter
        .upDownCounterBuilder(JFR_CPU_LOAD_JVM_USER)
        .ofDoubles()
        .setUnit(Constants.PERCENTAGE)
        .buildWithCallback(codm -> codm.observe(userValue));

    otelMeter
        .upDownCounterBuilder(JFR_CPU_LOAD_JVM_SYSTEM)
        .ofDoubles()
        .setUnit(Constants.PERCENTAGE)
        .buildWithCallback(codm -> codm.observe(systemValue));

    otelMeter
        .upDownCounterBuilder(JFR_CPU_LOAD_MACHINE_TOTAL)
        .ofDoubles()
        .setUnit(Constants.PERCENTAGE)
        .buildWithCallback(codm -> codm.observe(machineTotal));

    return this;
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(JVM_USER)) {
      userValue = ev.getDouble(JVM_USER);
    }
    if (ev.hasField(JVM_SYSTEM)) {
      systemValue = ev.getDouble(JVM_SYSTEM);
    }
    if (ev.hasField(MACHINE_TOTAL)) {
      machineTotal = ev.getDouble(MACHINE_TOTAL);
    }
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.of(1, ChronoUnit.SECONDS));
  }
}
