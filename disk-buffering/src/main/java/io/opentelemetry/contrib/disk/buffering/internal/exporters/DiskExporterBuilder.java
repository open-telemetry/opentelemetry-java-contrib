/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.FolderManager;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class DiskExporterBuilder<T> {

  private SignalSerializer<T> serializer =
      new SignalSerializer<T>() {

        @Override
        public byte[] serialize(Collection<T> ts) {
          return new byte[0];
        }

        @Override
        public List<T> deserialize(byte[] source) {
          return Collections.emptyList();
        }
      };
  private File rootDir = new File(".");
  private String folderName = "data";
  private StorageConfiguration configuration = StorageConfiguration.getDefault();
  private Clock clock = Clock.getDefault();

  private Function<Collection<T>, CompletableResultCode> exportFunction =
      x -> CompletableResultCode.ofFailure();

  DiskExporterBuilder() {}

  @CanIgnoreReturnValue
  public DiskExporterBuilder<T> setRootDir(File rootDir) {
    this.rootDir = rootDir;
    return this;
  }

  @CanIgnoreReturnValue
  public DiskExporterBuilder<T> setFolderName(String folderName) {
    this.folderName = folderName;
    return this;
  }

  @CanIgnoreReturnValue
  public DiskExporterBuilder<T> setStorageConfiguration(StorageConfiguration configuration) {
    this.configuration = configuration;
    return this;
  }

  @CanIgnoreReturnValue
  public DiskExporterBuilder<T> setStorageClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  @CanIgnoreReturnValue
  public DiskExporterBuilder<T> setSerializer(SignalSerializer<T> serializer) {
    this.serializer = serializer;
    return this;
  }

  @CanIgnoreReturnValue
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
