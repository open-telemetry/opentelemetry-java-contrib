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
import io.opentelemetry.sdk.metrics.data.MetricData;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class FileMetricStorage implements SignalStorage.Metric {
  private final FileSignalStorage<MetricData> fileSignalStorage;

  public static FileMetricStorage create(File destinationDir) {
    return create(destinationDir, FileStorageConfiguration.getDefault());
  }

  public static FileMetricStorage create(
      File destinationDir, FileStorageConfiguration configuration) {
    Storage<MetricData> storage =
        new Storage<>(FolderManager.create(destinationDir, configuration, Clock.getDefault()));
    return new FileMetricStorage(
        new FileSignalStorage<>(
            storage, SignalSerializer.ofMetrics(), SignalDeserializer.ofMetrics()));
  }

  private FileMetricStorage(FileSignalStorage<MetricData> fileSignalStorage) {
    this.fileSignalStorage = fileSignalStorage;
  }

  @Override
  public CompletableFuture<WriteResult> write(Collection<MetricData> items) {
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
  public Iterator<Collection<MetricData>> iterator() {
    return fileSignalStorage.iterator();
  }
}
