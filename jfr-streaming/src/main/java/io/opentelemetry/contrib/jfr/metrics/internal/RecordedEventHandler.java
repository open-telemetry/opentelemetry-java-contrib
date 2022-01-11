/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import jdk.jfr.consumer.RecordedEvent;

/** Convenience/Tag interface for defining how JFR events should turn into metrics. */
public interface RecordedEventHandler extends Consumer<RecordedEvent>, Predicate<RecordedEvent> {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  String getEventName();

  /**
   * Test to see if this event is interesting to this mapper
   *
   * @param event - event instance to see if we're interested
   * @return true if event is interesting, false otherwise
   */
  @Override
  default boolean test(RecordedEvent event) {
    return event.getEventType().getName().equalsIgnoreCase(getEventName());
  }

  /**
   * Set the OpenTelemetry {@link Meter} after the SDK has been initialized. Until called,
   * implementations should use instruments from {@link #defaultMeter()}.
   *
   * @param meter the meter
   */
  void initializeMeter(Meter meter);

  /**
   * Optionally returns a polling duration for JFR events, if present
   *
   * @return {@link Optional} of {@link Duration} representing polling duration; empty {@link
   *     Optional} if no polling
   */
  default Optional<Duration> getPollingDuration() {
    return Optional.empty();
  }

  /**
   * Optionally returns a threshold length for JFR events, if present
   *
   * @return {@link Optional} of {@link Duration} representing threshold; empty {@link Optional} if
   *     no threshold
   */
  default Optional<Duration> getThreshold() {
    return Optional.empty();
  }

  /** The default {@link Meter} to use until {@link #initializeMeter(Meter)} is called. */
  static Meter defaultMeter() {
    return MeterProvider.noop().get("noop");
  }
}
