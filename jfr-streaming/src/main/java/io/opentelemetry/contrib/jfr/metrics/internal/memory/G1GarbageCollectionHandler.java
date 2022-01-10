/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC_COLLECTOR;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.G1;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates the duration of G1 Garbage Collection JFR events */
public final class G1GarbageCollectionHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.G1GarbageCollection";
  private static final Attributes ATTR_G1 = Attributes.of(ATTR_GC_COLLECTOR, G1);

  private DoubleHistogram histogram;

  public G1GarbageCollectionHandler() {
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    histogram =
        meter
            .histogramBuilder("runtime.jvm.gc.duration")
            .setDescription("GC Duration")
            .setUnit(MILLISECONDS)
            .build();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    // FIXME Is this a getDuration, or is it named?
    histogram.record(ev.getDuration().toMillis(), ATTR_G1);
  }
}
