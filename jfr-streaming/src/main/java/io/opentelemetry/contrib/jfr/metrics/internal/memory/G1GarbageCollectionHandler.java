/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC_COLLECTOR;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.G1;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates the duration of G1 Garbage Collection JFR events */
public final class G1GarbageCollectionHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.G1GarbageCollection";
  private static final String METRIC_NAME = "runtime.jvm.gc.duration";
  private static final String DESCRIPTION = "GC Duration";

  private final BoundDoubleHistogram gcHistogram;

  public G1GarbageCollectionHandler(Meter otelMeter) {
    gcHistogram =
        otelMeter
            .histogramBuilder(METRIC_NAME)
            .setDescription(DESCRIPTION)
            .setUnit(Constants.MILLISECONDS)
            .build()
            .bind(Attributes.of(ATTR_GC_COLLECTOR, G1));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    // FIXME Is this a getDuration, or is it named?
    gcHistogram.record(ev.getDuration().toMillis());
  }
}
