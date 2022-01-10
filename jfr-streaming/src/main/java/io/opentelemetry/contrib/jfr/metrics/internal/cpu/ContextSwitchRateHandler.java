/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.HERTZ;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class ContextSwitchRateHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.ThreadContextSwitchRate";

  private volatile double value = 0;

  public ContextSwitchRateHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void accept(RecordedEvent ev) {
    value = ev.getDouble("switchRate");
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder("runtime.jvm.cpu.context_switch")
        .ofDoubles()
        .setUnit(HERTZ)
        .buildWithCallback(codm -> codm.record(value));
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
