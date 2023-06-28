/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.exporters.StoredBatchExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.FolderManager;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiskExporter<EXPORT_DATA> implements StoredBatchExporter {
  private final Storage storage;
  private final SignalSerializer<EXPORT_DATA> serializer;
  private final Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction;
  private static final Logger logger = Logger.getLogger(DiskExporter.class.getName());

  public DiskExporter(
      File rootDir,
      StorageConfiguration configuration,
      String folderName,
      SignalSerializer<EXPORT_DATA> serializer,
      Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction) {
    validateConfiguration(configuration);
    this.storage =
        new Storage(new FolderManager(getSignalFolder(rootDir, folderName), configuration));
    this.serializer = serializer;
    this.exportFunction = exportFunction;
  }

  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    logger.log(Level.INFO, "Attempting to export batch from disk.");
    AtomicBoolean exportSucceeded = new AtomicBoolean(false);
    boolean foundDataToExport =
        storage.read(
            bytes -> {
              logger.log(Level.INFO, "About to export stored batch.");
              CompletableResultCode join =
                  exportFunction.apply(serializer.deserialize(bytes)).join(timeout, unit);
              exportSucceeded.set(join.isSuccess());
              return exportSucceeded.get();
            });
    return foundDataToExport && exportSucceeded.get();
  }

  public void onShutDown() throws IOException {
    storage.close();
  }

  public CompletableResultCode onExport(Collection<EXPORT_DATA> data) {
    logger.log(Level.FINER, "Intercepting exporter batch.");
    try {
      storage.write(serializer.serialize(data));
      return CompletableResultCode.ofSuccess();
    } catch (IOException e) {
      logger.log(Level.INFO, "Could not store batch in disk. Exporting it right away.");
      return exportFunction.apply(data);
    }
  }

  private static File getSignalFolder(File rootDir, String folderName) {
    File folder = new File(rootDir, folderName);
    if (!folder.exists()) {
      if (!folder.mkdirs()) {
        throw new IllegalStateException("Could not create the signal folder");
      }
    }
    return folder;
  }

  private static void validateConfiguration(StorageConfiguration configuration) {
    if (configuration.getMinFileAgeForReadMillis() <= configuration.getMaxFileAgeForWriteMillis()) {
      throw new IllegalArgumentException(
          "The configured max file age for writing must be lower than the configured min file age for reading");
    }
  }
}
