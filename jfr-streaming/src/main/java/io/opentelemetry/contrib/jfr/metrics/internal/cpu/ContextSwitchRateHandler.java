/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HERTZ;

import io.opentelemetry.api.metrics.*;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class ContextSwitchRateHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.ThreadContextSwitchRate";
  private static final String METRIC_NAME = "runtime.jvm.cpu.context_switch";

  private volatile double value = 0;

  public ContextSwitchRateHandler(Meter otelMeter) {
    otelMeter
        .upDownCounterBuilder(METRIC_NAME)
        .ofDoubles()
        .setUnit(HERTZ)
        .buildWithCallback(codm -> codm.observe(value));
  }

  @Override
  public void accept(RecordedEvent ev) {
    value = ev.getDouble("switchRate");
  }

  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
