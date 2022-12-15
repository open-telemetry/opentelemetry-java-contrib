/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.GarbageCollection;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.END_OF_MINOR_GC;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_GC_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.HandlerRegistry;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class YoungGarbageCollectionHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.YoungGarbageCollection";
  private Attributes attributes =
      Attributes.of(ATTR_GC, "PS Scavenge", ATTR_ACTION, END_OF_MINOR_GC);
  private LongHistogram histogram;

  public YoungGarbageCollectionHandler() {
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
    // G1 young collection is already handled by G1GarbageCollectionHandler.
    if (HandlerRegistry.garbageCollectors.contains("PS Scavenge")) {
      attributes = Attributes.of(ATTR_GC, "PS Scavenge", ATTR_ACTION, END_OF_MINOR_GC);
    } else if (HandlerRegistry.garbageCollectors.contains("Copy")) {
      attributes = Attributes.of(ATTR_GC, "Copy", ATTR_ACTION, END_OF_MINOR_GC);
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
