/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_TYPE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.AVERAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.COUNT;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HERTZ;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MACHINE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MAX;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.SYSTEM;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USER;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class OverallCPULoadHandler implements RecordedEventHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.cpu.used";
  private static final String METRIC_DESCRIPTION = "CPU Utilization";
  private static final String EVENT_NAME = "jdk.CPULoad";
  private static final String JVM_USER = "user";
  private static final String JVM_SYSTEM = "system";
  private static final String MACHINE_TOTAL = "machine.total";

  private static final Attributes ATTR_USER_AVERAGE =
      Attributes.of(ATTR_USAGE, USER, ATTR_TYPE, AVERAGE);
  private static final Attributes ATTR_USER_COUNT =
      Attributes.of(ATTR_USAGE, USER, ATTR_TYPE, COUNT);
  private static final Attributes ATTR_USER_MAX = Attributes.of(ATTR_USAGE, USER, ATTR_TYPE, MAX);
  private static final Attributes ATTR_SYSTEM_AVERAGE =
      Attributes.of(ATTR_USAGE, SYSTEM, ATTR_TYPE, AVERAGE);
  private static final Attributes ATTR_SYSTEM_COUNT =
      Attributes.of(ATTR_USAGE, SYSTEM, ATTR_TYPE, COUNT);
  private static final Attributes ATTR_SYSTEM_MAX =
      Attributes.of(ATTR_USAGE, SYSTEM, ATTR_TYPE, MAX);
  private static final Attributes ATTR_MACHINE_AVERAGE =
      Attributes.of(ATTR_USAGE, MACHINE, ATTR_TYPE, AVERAGE);
  private static final Attributes ATTR_MACHINE_COUNT =
      Attributes.of(ATTR_USAGE, MACHINE, ATTR_TYPE, COUNT);
  private static final Attributes ATTR_MACHINE_MAX =
      Attributes.of(ATTR_USAGE, MACHINE, ATTR_TYPE, MAX);

  private final List<Double> jvmUserData = new ArrayList<>();
  private final List<Double> jvmSystemData = new ArrayList<>();
  private final List<Double> machineTotalData = new ArrayList<>();

  public OverallCPULoadHandler() {
    initializeMeter(defaultMeter());
  }

  private static DoubleSummaryStatistics summarize(List<Double> l) {
    return l.stream().mapToDouble(x -> x).summaryStatistics();
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME)
        .ofDoubles()
        .setUnit(HERTZ)
        .setDescription(METRIC_DESCRIPTION)
        .buildWithCallback(
            codm -> {
              var summary = summarize(jvmUserData);
              codm.record(summary.getAverage(), ATTR_USER_AVERAGE);
              codm.record(summary.getCount(), ATTR_USER_COUNT);
              codm.record(summary.getMax(), ATTR_USER_MAX);
              jvmUserData.clear();
              summary = summarize(jvmSystemData);
              codm.record(summary.getAverage(), ATTR_SYSTEM_AVERAGE);
              codm.record(summary.getCount(), ATTR_SYSTEM_COUNT);
              codm.record(summary.getMax(), ATTR_SYSTEM_MAX);
              jvmSystemData.clear();
              summary = summarize(machineTotalData);
              codm.record(summary.getAverage(), ATTR_MACHINE_AVERAGE);
              codm.record(summary.getCount(), ATTR_MACHINE_COUNT);
              codm.record(summary.getMax(), ATTR_MACHINE_MAX);
              machineTotalData.clear();
            });
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(JVM_USER)) {
      jvmUserData.add(ev.getDouble(JVM_USER));
    }
    if (ev.hasField(JVM_SYSTEM)) {
      jvmSystemData.add(ev.getDouble(JVM_SYSTEM));
    }
    if (ev.hasField(MACHINE_TOTAL)) {
      machineTotalData.add(ev.getDouble(MACHINE_TOTAL));
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
