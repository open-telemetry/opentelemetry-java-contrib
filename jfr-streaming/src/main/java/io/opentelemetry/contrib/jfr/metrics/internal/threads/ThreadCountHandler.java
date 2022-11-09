/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.threads;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_DAEMON;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.FALSE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.TRUE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_THREADS;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class ThreadCountHandler implements RecordedEventHandler {
  private static final String METRIC_NAME = "process.runtime.jvm.threads.count";
  private static final String EVENT_NAME = "jdk.JavaThreadStatistics";
  private static final String METRIC_DESCRIPTION = "Number of executing threads";
  private static final Attributes ATTR_DAEMON_TRUE = Attributes.of(ATTR_DAEMON, TRUE);
  private static final Attributes ATTR_DAEMON_FALSE = Attributes.of(ATTR_DAEMON, FALSE);
  private volatile long activeCount = 0;
  private volatile long daemonCount = 0;

  public ThreadCountHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void accept(RecordedEvent ev) {
    activeCount = ev.getLong("activeCount");
    daemonCount = ev.getLong("daemonCount");
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void initializeMeter(Meter meter) {
    meter
        .upDownCounterBuilder(METRIC_NAME)
        .setDescription(METRIC_DESCRIPTION)
        .setUnit(UNIT_THREADS)
        .buildWithCallback(
            measurement -> {
              measurement.record(daemonCount, ATTR_DAEMON_TRUE);
              measurement.record(activeCount - daemonCount, ATTR_DAEMON_FALSE);
            });
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
