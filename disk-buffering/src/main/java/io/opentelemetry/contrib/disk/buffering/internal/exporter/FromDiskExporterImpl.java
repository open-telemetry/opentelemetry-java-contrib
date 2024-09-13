/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Signal-type generic class that can read telemetry previously buffered on disk and send it to
 * another delegated exporter.
 */
public final class FromDiskExporterImpl<EXPORT_DATA> implements FromDiskExporter {
  private static final Logger logger = Logger.getLogger(FromDiskExporterImpl.class.getName());
  private final Storage storage;
  private final SignalDeserializer<EXPORT_DATA> deserializer;
  private final Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction;
  private final boolean debugEnabled;

  FromDiskExporterImpl(
      SignalDeserializer<EXPORT_DATA> deserializer,
      Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction,
      Storage storage,
      boolean debugEnabled) {
    this.deserializer = deserializer;
    this.exportFunction = exportFunction;
    this.storage = storage;
    this.debugEnabled = debugEnabled;
  }

  public static <T> FromDiskExporterBuilder<T> builder() {
    return new FromDiskExporterBuilder<>();
  }

  /**
   * Reads data from the disk and attempts to export it.
   *
   * @param timeout The amount of time to wait for the wrapped exporter to finish.
   * @param unit The unit of the time provided.
   * @return true if there was data available, and it was successfully exported within the timeout
   *     provided. false otherwise.
   * @throws IOException If an unexpected error happens.
   */
  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    log("Attempting to export " + deserializer.signalType() + " batch from disk.");
    ReadableResult result =
        storage.readAndProcess(
            bytes -> {
              log(
                  "Read "
                      + bytes.length
                      + " "
                      + deserializer.signalType()
                      + " bytes from storage.");
              List<EXPORT_DATA> telemetry = deserializer.deserialize(bytes);
              log("Now exporting batch of " + telemetry.size() + " " + deserializer.signalType());
              CompletableResultCode join = exportFunction.apply(telemetry).join(timeout, unit);
              return join.isSuccess();
            });
    return result == ReadableResult.SUCCEEDED;
  }

  private void log(String msg) {
    if (debugEnabled) {
      logger.log(Level.INFO, msg);
    }
  }

  @Override
  public void shutdown() throws IOException {
    storage.close();
  }
}
