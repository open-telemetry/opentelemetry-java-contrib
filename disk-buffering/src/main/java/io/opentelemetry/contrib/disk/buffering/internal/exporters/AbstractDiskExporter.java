/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

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
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractDiskExporter<EXPORT_DATA> {
  private final Storage storage;
  private static final Logger logger = Logger.getLogger(AbstractDiskExporter.class.getName());

  public AbstractDiskExporter(File rootDir, StorageConfiguration configuration) {
    validateConfiguration(configuration);
    this.storage = new Storage(new FolderManager(getSignalFolder(rootDir), configuration));
  }

  /**
   * Reads data from the disk and attempts to export it.
   *
   * @param timeout The amount of time to wait for the wrapped exporter to finish.
   * @param unit The unit of the time provided.
   * @return TRUE if there was data available and it was successfully exported within the timeout
   *     provided. FALSE if either of those conditions didn't meet.
   * @throws IOException If an unexpected error happens.
   */
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    logger.log(Level.INFO, "Attempting to export batch from disk.");
    AtomicBoolean exportSucceeded = new AtomicBoolean(false);
    boolean foundDataToExport =
        storage.read(
            bytes -> {
              logger.log(Level.INFO, "About to export stored batch.");
              CompletableResultCode join =
                  doExport(getSerializer().deserialize(bytes)).join(timeout, unit);
              exportSucceeded.set(join.isSuccess());
              return exportSucceeded.get();
            });
    return foundDataToExport && exportSucceeded.get();
  }

  protected abstract String getStorageFolderName();

  protected abstract CompletableResultCode doExport(Collection<EXPORT_DATA> data);

  protected abstract SignalSerializer<EXPORT_DATA> getSerializer();

  protected void onShutDown() throws IOException {
    storage.close();
  }

  protected CompletableResultCode onExport(Collection<EXPORT_DATA> data) {
    logger.log(Level.DEBUG, "Intercepting exporter batch.");
    try {
      storage.write(getSerializer().serialize(data));
      return CompletableResultCode.ofSuccess();
    } catch (IOException e) {
      logger.log(Level.INFO, "Could not store batch in disk. Exporting it right away.");
      return doExport(data);
    }
  }

  private File getSignalFolder(File rootDir) {
    File folder = new File(rootDir, getStorageFolderName());
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
