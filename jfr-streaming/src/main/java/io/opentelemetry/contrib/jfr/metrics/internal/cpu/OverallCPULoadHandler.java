/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_USAGE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HERTZ;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MACHINE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.SYSTEM;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.USER;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import jdk.jfr.consumer.RecordedEvent;

public final class OverallCPULoadHandler implements RecordedEventHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.cpu.used";
  private static final String METRIC_DESCRIPTION = "CPU Utilization";
  private static final String EVENT_NAME = "jdk.CPULoad";
  private static final String JVM_USER = "user";
  private static final String JVM_SYSTEM = "system";
  private static final String MACHINE_TOTAL = "machine.total";

  private static final Attributes ATTR_USER = Attributes.of(ATTR_USAGE, USER);
  private static final Attributes ATTR_SYSTEM = Attributes.of(ATTR_USAGE, SYSTEM);
  private static final Attributes ATTR_MACHINE = Attributes.of(ATTR_USAGE, MACHINE);

  private final List<Double> jvmUserData = new ArrayList<>();
  private final List<Double> jvmSystemData = new ArrayList<>();
  private final List<Double> machineTotalData = new ArrayList<>();

  @SuppressWarnings("UnnecessaryLambda")
  private static final Function<List<Double>, Double> AVERAGE =
      l -> l.stream().mapToDouble(x -> x).summaryStatistics().getAverage();

  public OverallCPULoadHandler() {
    initializeMeter(defaultMeter());
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
              codm.record(AVERAGE.apply(jvmUserData), ATTR_USER);
              codm.record(AVERAGE.apply(jvmSystemData), ATTR_SYSTEM);
              codm.record(AVERAGE.apply(machineTotalData), ATTR_MACHINE);
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
