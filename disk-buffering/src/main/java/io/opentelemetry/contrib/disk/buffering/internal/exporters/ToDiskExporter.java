/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ToDiskExporter<EXPORT_DATA> {

  private static final Logger logger = Logger.getLogger(ToDiskExporter.class.getName());
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
  }

  public static <T> ToDiskExporterBuilder<T> builder() {
    return new ToDiskExporterBuilder<>();
  }

  public CompletableResultCode export(Collection<EXPORT_DATA> data) {
    logger.log(Level.FINER, "Intercepting exporter batch.");
    try {
      if (storage.write(serializer.serialize(data))) {
        return CompletableResultCode.ofSuccess();
      }
      logger.log(Level.INFO, "Could not store batch in disk. Exporting it right away.");
      return exportFunction.apply(data);
    } catch (IOException e) {
      logger.log(
          Level.WARNING,
          "An unexpected error happened while attempting to write the data in disk. Exporting it right away.",
          e);
      return exportFunction.apply(data);
    }
  }

  public void shutdown() throws IOException {
    storage.close();
  }
}
