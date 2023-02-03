/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming.internal.GarbageCollection;

import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.DURATION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.END_OF_MINOR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_NAME_GC_DURATION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.MILLISECONDS;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.streaming.JfrFeature;
import io.opentelemetry.contrib.jfr.streaming.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class G1GarbageCollectionHandler implements RecordedEventHandler {
  private static final String EVENT_NAME = "jdk.G1GarbageCollection";
  private static final Attributes ATTR =
      Attributes.of(ATTR_GC, "G1 Young Generation", ATTR_ACTION, END_OF_MINOR_GC);
  private final LongHistogram histogram;

  public G1GarbageCollectionHandler(Meter meter) {
    histogram =
        meter
            .histogramBuilder(METRIC_NAME_GC_DURATION)
            .setDescription(METRIC_DESCRIPTION_GC_DURATION)
            .setUnit(MILLISECONDS)
            .ofLongs()
            .build();
  }

  @Override
  public void accept(RecordedEvent ev) {
    histogram.record(ev.getLong(DURATION), ATTR);
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public JfrFeature getFeature() {
    return JfrFeature.GC_DURATION_METRICS;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }

  @Override
  public void close() {}
}
