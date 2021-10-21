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
  private static final String JFR_G1_GARBAGE_COLLECTION_DURATION = "jvm.runtime.gc.time";
  private static final String DESCRIPTION = "G1 GC Duration";

  private final Meter otelMeter;
  private BoundDoubleHistogram gcHistogram;

  public G1GarbageCollectionHandler(Meter otelMeter) {
    this.otelMeter = otelMeter;
  }

  public G1GarbageCollectionHandler init() {
    var attr = Attributes.of(ATTR_GC_COLLECTOR, G1);
    var builder = otelMeter.histogramBuilder(JFR_G1_GARBAGE_COLLECTION_DURATION);
    builder.setDescription(DESCRIPTION);
    builder.setUnit(Constants.MILLISECONDS);
    gcHistogram = builder.build().bind(attr);
    return this;
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
