/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.container;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ONE;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

public final class ContainerConfigurationHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.ContainerConfiguration";
  private static final String EFFECTIVE_CPU_COUNT = "effectiveCpuCount";

  private volatile long value = 0L;

  public ContainerConfigurationHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder("runtime.jvm.cpu.limit")
        .ofDoubles()
        .setUnit(ONE)
        .buildWithCallback(codm -> codm.record(value));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(EFFECTIVE_CPU_COUNT)) {
      value = ev.getLong(EFFECTIVE_CPU_COUNT);
    }
  }
}
