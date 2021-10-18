/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.memory;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.ThreadGrouper;

/**
 * This class handles all non-TLAB allocation JFR events, and delegates them to the actual
 * per-thread aggregators
 */
public final class ObjectAllocationOutsideTLABHandler extends AbstractThreadDispatchingHandler {

  public static final String EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  private final Meter otelMeter;

  public ObjectAllocationOutsideTLABHandler(Meter otelMeter, ThreadGrouper grouper) {
    super(grouper);
    this.otelMeter = otelMeter;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  public RecordedEventHandler createPerThreadSummarizer(String threadName) {
    var ret = new PerThreadObjectAllocationOutsideTLABHandler(otelMeter, threadName);
    return ret.init();
  }
}
