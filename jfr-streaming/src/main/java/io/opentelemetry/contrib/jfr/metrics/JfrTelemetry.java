/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.io.Closeable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.jfr.consumer.RecordingStream;

/** The entry point class for the JFR-over-OpenTelemetry support. */
public final class JfrTelemetry implements Closeable {

  private static final Logger logger = Logger.getLogger(JfrTelemetry.class.getName());

  private final AtomicBoolean isClosed = new AtomicBoolean();
  private final RecordingStream recordingStream;

  @SuppressWarnings("CatchingUnchecked")
  private JfrTelemetry(OpenTelemetry openTelemetry) {
    List<RecordedEventHandler> handlers = HandlerRegistry.getHandlers(openTelemetry);
    try {
      logger.log(Level.INFO, "Starting JfrTelemetry");
      recordingStream = new RecordingStream();
      handlers.forEach(
          handler -> {
            var eventSettings = recordingStream.enable(handler.getEventName());
            handler.getPollingDuration().ifPresent(eventSettings::withPeriod);
            handler.getThreshold().ifPresent(eventSettings::withThreshold);
            recordingStream.onEvent(handler.getEventName(), handler);
          });
      recordingStream.startAsync();
    } catch (Exception e) {
      close();
      throw new IllegalStateException("Error starting JfrTelemetry", e);
    }
  }

  /**
   * Create and start {@link JfrTelemetry}.
   *
   * <p>Listens for select JFR events, extracts data, and records to various metrics. Recording will
   * continue until {@link #close()} is called.
   */
  public static JfrTelemetry create(OpenTelemetry openTelemetry) {
    return new JfrTelemetry(openTelemetry);
  }

  // Visible for testing
  RecordingStream getRecordingStream() {
    return recordingStream;
  }

  /** Stop recording JFR events. */
  @Override
  public void close() {
    if (!isClosed.compareAndSet(false, true)) {
      logger.log(Level.WARNING, "JfrTelemetry is already closed");
      return;
    }
    logger.log(Level.INFO, "Closing JfrTelemetry");
    recordingStream.close();
  }
}
