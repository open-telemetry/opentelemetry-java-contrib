/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.utils.DebugLogger;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import java.util.logging.Logger;

public class ToDiskExporter<EXPORT_DATA> {

  private final DebugLogger logger;
  private final Storage storage;
  private final SignalSerializer<EXPORT_DATA> serializer;
  private final Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction;

  ToDiskExporter(
      SignalSerializer<EXPORT_DATA> serializer,
      Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction,
      Storage storage) {
    this.serializer = serializer;
    this.exportFunction = exportFunction;
    this.storage = storage;
    this.logger =
        DebugLogger.wrap(
            Logger.getLogger(ToDiskExporter.class.getName()), storage.isDebugEnabled());
  }

  public static <T> ToDiskExporterBuilder<T> builder(Storage storage) {
    return new ToDiskExporterBuilder<>(storage);
  }

  public synchronized CompletableResultCode export(Collection<EXPORT_DATA> data) {
    logger.log("Intercepting exporter batch.", FINER);
    try {
      serializer.initialize(data);
      if (storage.write(serializer)) {
        return CompletableResultCode.ofSuccess();
      }
      logger.log("Could not store batch in disk. Exporting it right away.");
      return exportFunction.apply(data);
    } catch (IOException e) {
      logger.log(
          "An unexpected error happened while attempting to write the data in disk. Exporting it right away.",
          WARNING,
          e);
      return exportFunction.apply(data);
    } finally {
      serializer.reset();
    }
  }

  public void shutdown() throws IOException {
    storage.close();
  }
}
