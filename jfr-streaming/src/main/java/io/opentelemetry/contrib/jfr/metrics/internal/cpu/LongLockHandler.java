/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.cpu;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.time.Duration;
import java.util.Optional;

public final class LongLockHandler extends AbstractThreadDispatchingHandler {
  static final String EVENT_NAME = "jdk.JavaMonitorWait";
  private final Meter otelMeter;
  private static final String METRIC_NAME = "runtime.jvm.longlock.time";
  private static final String DESCRIPTION = "Long lock times";

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
    var attr = Attributes.of(ATTR_THREAD_NAME, threadName);
    var builder = otelMeter.histogramBuilder(METRIC_NAME);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(Constants.MILLISECONDS);
    var histogram = builder.build().bind(attr);
    var ret = new PerThreadLongLockHandler(histogram, threadName);
    return ret.init();
  }

  @Override
  public Optional<Duration> getThreshold() {
    return Optional.empty();
  }
}
