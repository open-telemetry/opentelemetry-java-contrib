/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import io.opentelemetry.contrib.jfr.metrics.JfrFeature;
import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import jdk.jfr.consumer.RecordedEvent;

/** Convenience/Tag interface for defining how JFR events should turn into metrics. */
public interface RecordedEventHandler
    extends Consumer<RecordedEvent>, Predicate<RecordedEvent>, Closeable {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  String getEventName();

  /** Return the {@link JfrFeature} this handler is associated with. */
  JfrFeature getFeature();

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

  default void closeObservables(List<AutoCloseable> observables) {
    observables.forEach(
        observable -> {
          try {
            observable.close();
          } catch (Exception e) {
            // Ignore
          }
        });
  }
}
