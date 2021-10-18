/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.cpu;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.ThreadGrouper;
import java.time.Duration;
import java.util.Optional;

public class LongLockHandler extends AbstractThreadDispatchingHandler {

  public static final String EVENT_NAME = "jdk.JavaMonitorWait";
  private final Meter otelMeter;

  public LongLockHandler(Meter otelMeter, ThreadGrouper grouper) {
    super(grouper);
    this.otelMeter = otelMeter;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public RecordedEventHandler createPerThreadSummarizer(String threadName) {
    var ret = new PerThreadLongLockHandler(otelMeter, threadName);
    return ret.init();
  }

  @Override
  public Optional<Duration> getThreshold() {
    return Optional.empty();
  }
}
