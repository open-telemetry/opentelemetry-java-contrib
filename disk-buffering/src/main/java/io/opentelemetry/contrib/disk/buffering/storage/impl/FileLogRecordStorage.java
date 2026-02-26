/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage.impl;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.FileSignalStorage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.FolderManager;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class FileLogRecordStorage implements SignalStorage.LogRecord {
  private final FileSignalStorage<LogRecordData> fileSignalStorage;

  public static FileLogRecordStorage create(File destinationDir) {
    return create(destinationDir, FileStorageConfiguration.getDefault());
  }

  public static FileLogRecordStorage create(
      File destinationDir, FileStorageConfiguration configuration) {
    Storage<LogRecordData> storage =
        new Storage<>(FolderManager.create(destinationDir, configuration, Clock.getDefault()));
    return new FileLogRecordStorage(
        new FileSignalStorage<>(
            storage,
            SignalSerializer.ofLogs(),
            SignalDeserializer.ofLogs(),
            configuration.getDeleteItemsOnIteration()));
  }

  private FileLogRecordStorage(FileSignalStorage<LogRecordData> fileSignalStorage) {
    this.fileSignalStorage = fileSignalStorage;
  }

  @Override
  public CompletableFuture<WriteResult> write(Collection<LogRecordData> items) {
    return fileSignalStorage.write(items);
  }

  @Override
  public CompletableFuture<WriteResult> clear() {
    return fileSignalStorage.clear();
  }

  @Override
  public void close() throws IOException {
    fileSignalStorage.close();
  }

  @Nonnull
  @Override
  public Iterator<Collection<LogRecordData>> iterator() {
    return fileSignalStorage.iterator();
  }
}
