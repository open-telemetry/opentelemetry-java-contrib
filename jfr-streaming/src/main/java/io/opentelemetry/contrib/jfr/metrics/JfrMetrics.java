/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.jfr.EventSettings;
import jdk.jfr.consumer.RecordingStream;

/** The entry point class for the JFR-over-OpenTelemetry support. */
public final class JfrMetrics {
  private JfrMetrics() {}

  private static final Logger logger = Logger.getLogger(JfrMetrics.class.getName());

  /**
   * Enables and starts a JFR recording stream on a background thread. The thread converts a subset
   * of JFR events to OpenTelemetry metrics.
   *
   * @param meterProvider - the OpenTelemetry metric provider that will harvest the generated
   *     metrics.
   */
  public static void enable(MeterProvider meterProvider) {
    var jfrMonitorService = Executors.newSingleThreadExecutor();
    var toMetricRegistry = HandlerRegistry.createDefault(meterProvider);

    jfrMonitorService.submit(
        () -> {
          try (var recordingStream = new RecordingStream()) {
            var enableMappedEvent = eventEnablerFor(recordingStream);
            toMetricRegistry.all().forEach(enableMappedEvent);
            recordingStream.setReuse(false);
            logger.log(Level.FINE, "Starting recording stream...");
            recordingStream.start(); // run forever
          }
        });
  }

  private static Consumer<RecordedEventHandler> eventEnablerFor(RecordingStream recordingStream) {
    return handler -> {
      EventSettings eventSettings = recordingStream.enable(handler.getEventName());
      handler.getPollingDuration().ifPresent(eventSettings::withPeriod);
      handler.getThreshold().ifPresent(eventSettings::withThreshold);
      recordingStream.onEvent(handler.getEventName(), handler);
    };
  }
}
