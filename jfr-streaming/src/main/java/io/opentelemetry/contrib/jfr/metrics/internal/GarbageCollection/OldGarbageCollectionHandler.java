/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.GarbageCollection;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.END_OF_MAJOR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_GC_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class OldGarbageCollectionHandler implements RecordedEventHandler {
  private final HashSet<String> garbageCollectors;
  private static final String EVENT_NAME = "jdk.OldGarbageCollection";
  // This could be changed later
  private Attributes attributes =
      Attributes.of(ATTR_GC, "MarkSweepCompact", ATTR_ACTION, END_OF_MAJOR_GC);

  private LongHistogram histogram;

  public OldGarbageCollectionHandler(HashSet<String> garbageCollectors) {
    this.garbageCollectors = garbageCollectors;
    initializeMeter(defaultMeter());
  }

  @Override
  public void accept(RecordedEvent ev) {
    histogram.record(ev.getLong(DURATION), attributes);
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void initializeMeter(Meter meter) {
    // Set the attribute's GC based on which GC is being used.
    if (garbageCollectors.contains("PS MarkSweep")) {
      attributes = Attributes.of(ATTR_GC, "PS MarkSweep", ATTR_ACTION, END_OF_MAJOR_GC);
    } else if (garbageCollectors.contains("G1 Old Generation")) {
      attributes = Attributes.of(ATTR_GC, "G1 Old Generation", ATTR_ACTION, END_OF_MAJOR_GC);
    } else if (garbageCollectors.contains("MarkSweepCompact")) {
      attributes = Attributes.of(ATTR_GC, "MarkSweepCompact", ATTR_ACTION, END_OF_MAJOR_GC);
    }

    histogram =
        meter
            .histogramBuilder(METRIC_NAME_GC_DURATION)
            .setDescription(METRIC_DESCRIPTION_GC_DURATION)
            .setUnit(MILLISECONDS)
            .ofLongs()
            .build();
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }
}
