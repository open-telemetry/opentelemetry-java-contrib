/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ProcessResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.utils.DebugLogger;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Signal-type generic class that can read telemetry previously buffered on disk and send it to
 * another delegated exporter.
 */
public class FromDiskExporterImpl<EXPORT_DATA> implements FromDiskExporter {
  private final DebugLogger logger;
  private final Storage storage;
  private final SignalDeserializer<EXPORT_DATA> deserializer;
  private final Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction;

  FromDiskExporterImpl(
      SignalDeserializer<EXPORT_DATA> deserializer,
      Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction,
      Storage storage) {
    this.deserializer = deserializer;
    this.exportFunction = exportFunction;
    this.storage = storage;
    this.logger =
        DebugLogger.wrap(
            Logger.getLogger(FromDiskExporterImpl.class.getName()), storage.isDebugEnabled());
  }

  public static <T> FromDiskExporterBuilder<T> builder(Storage storage) {
    return new FromDiskExporterBuilder<>(storage);
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
    logger.log("Attempting to export " + deserializer.signalType() + " batch from disk.");
    ReadableResult result =
        storage.readAndProcess(
            bytes -> {
              logger.log(
                  "Read "
                      + bytes.length
                      + " "
                      + deserializer.signalType()
                      + " bytes from storage.");
              try {
                List<EXPORT_DATA> telemetry = deserializer.deserialize(bytes);
                logger.log(
                    "Now exporting batch of " + telemetry.size() + " " + deserializer.signalType());
                CompletableResultCode join = exportFunction.apply(telemetry).join(timeout, unit);
                return join.isSuccess() ? ProcessResult.SUCCEEDED : ProcessResult.TRY_LATER;
              } catch (DeserializationException e) {
                return ProcessResult.CONTENT_INVALID;
              }
            });
    return result == ReadableResult.SUCCEEDED;
  }

  @Override
  public void shutdown() throws IOException {
    storage.close();
  }
}
