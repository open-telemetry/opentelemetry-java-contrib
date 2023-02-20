/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.container;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ONE;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.JfrFeature;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

public final class ContainerConfigurationHandler implements RecordedEventHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.cpu.limit";
  private static final String EVENT_NAME = "jdk.ContainerConfiguration";
  private static final String EFFECTIVE_CPU_COUNT = "effectiveCpuCount";

  private final List<AutoCloseable> observables = new ArrayList<>();

  private volatile long value = 0L;

  public ContainerConfigurationHandler(Meter meter) {
    observables.add(
        meter
            .upDownCounterBuilder(METRIC_NAME)
            .setUnit(ONE)
            .buildWithCallback(codm -> codm.record(value)));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public JfrFeature getFeature() {
    return JfrFeature.CPU_COUNT_METRICS;
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(EFFECTIVE_CPU_COUNT)) {
      value = ev.getLong(EFFECTIVE_CPU_COUNT);
    }
  }

  @Override
  public void close() {
    closeObservables(observables);
  }
}
