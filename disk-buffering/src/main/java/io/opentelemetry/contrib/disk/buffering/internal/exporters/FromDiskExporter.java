/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Signal-type generic class that can read telemetry previously buffered on disk and send it to
 * another delegated exporter.
 */
public final class FromDiskExporter<EXPORT_DATA> {
  private final Storage storage;
  private final SignalSerializer<EXPORT_DATA> deserializer;
  private final Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction;
  private static final Logger logger = Logger.getLogger(FromDiskExporter.class.getName());

  FromDiskExporter(
      SignalSerializer<EXPORT_DATA> deserializer,
      Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction,
      Storage storage) {
    this.deserializer = deserializer;
    this.exportFunction = exportFunction;
    this.storage = storage;
  }

  public static <T> FromDiskExporterBuilder<T> builder() {
    return new FromDiskExporterBuilder<>();
  }

  /**
   * Reads data from the disk and attempts to export it.
   *
   * @param timeout The amount of time to wait for the wrapped exporter to finish.
   * @param unit The unit of the time provided.
   * @return true if there was data available and it was successfully exported within the timeout
   *     provided. false otherwise.
   * @throws IOException If an unexpected error happens.
   */
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    logger.log(Level.INFO, "Attempting to export batch from disk.");
    ReadableResult result =
        storage.readAndProcess(
            bytes -> {
              logger.log(Level.INFO, "About to export stored batch.");
              CompletableResultCode join =
                  exportFunction.apply(deserializer.deserialize(bytes)).join(timeout, unit);
              return join.isSuccess();
            });
    return result == ReadableResult.SUCCEEDED;
  }

  public void onShutDown() throws IOException {
    storage.close();
  }
}
