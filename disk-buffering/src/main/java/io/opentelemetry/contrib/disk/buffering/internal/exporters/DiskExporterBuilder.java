/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.FolderManager;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.StorageClock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

public final class DiskExporterBuilder<T> {

  private SignalSerializer<T> serializer;
  private File rootDir;
  private String folderName;
  private StorageConfiguration configuration;
  private StorageClock clock = StorageClock.getInstance();
  private Function<Collection<T>, CompletableResultCode> exportFunction;

  DiskExporterBuilder() {}

  public DiskExporterBuilder<T> setRootDir(File rootDir) {
    this.rootDir = rootDir;
    return this;
  }

  public DiskExporterBuilder<T> setFolderName(String folderName) {
    this.folderName = folderName;
    return this;
  }

  public DiskExporterBuilder<T> setStorageConfiguration(StorageConfiguration configuration) {
    this.configuration = configuration;
    return this;
  }

  public DiskExporterBuilder<T> setStorageClock(StorageClock clock) {
    this.clock = clock;
    return this;
  }

  public DiskExporterBuilder<T> setSerializer(SignalSerializer<T> serializer) {
    this.serializer = serializer;
    return this;
  }

  public DiskExporterBuilder<T> setExportFunction(
      Function<Collection<T>, CompletableResultCode> exportFunction) {
    this.exportFunction = exportFunction;
    return this;
  }

  private static File getSignalFolder(File rootDir, String folderName) throws IOException {
    File folder = new File(rootDir, folderName);
    if (!folder.exists()) {
      if (!folder.mkdirs()) {
        throw new IOException(
            "Could not create the signal folder: '" + folderName + "' inside: " + rootDir);
      }
    }
    return folder;
  }

  public DiskExporter<T> build() throws IOException {
    validateConfiguration(configuration);

    File folder = getSignalFolder(rootDir, folderName);
    Storage storage = new Storage(new FolderManager(folder, configuration, clock));

    return new DiskExporter<>(serializer, exportFunction, storage);
  }

  private static void validateConfiguration(StorageConfiguration configuration) {
    if (configuration.getMinFileAgeForReadMillis() <= configuration.getMaxFileAgeForWriteMillis()) {
      throw new IllegalArgumentException(
          "The configured max file age for writing must be lower than the configured min file age for reading");
    }
  }
}
