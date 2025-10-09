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
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class FileSpanStorage implements SignalStorage.Span {
  private final FileSignalStorage<SpanData> fileSignalStorage;

  public static FileSpanStorage create(File destinationDir) {
    return create(destinationDir, FileStorageConfiguration.getDefault());
  }

  public static FileSpanStorage create(
      File destinationDir, FileStorageConfiguration configuration) {
    Storage<SpanData> storage =
        new Storage<>(FolderManager.create(destinationDir, configuration, Clock.getDefault()));
    return new FileSpanStorage(
        new FileSignalStorage<>(storage, SignalSerializer.ofSpans(), SignalDeserializer.ofSpans()));
  }

  private FileSpanStorage(FileSignalStorage<SpanData> fileSignalStorage) {
    this.fileSignalStorage = fileSignalStorage;
  }

  @Override
  public CompletableFuture<WriteResult> write(Collection<SpanData> items) {
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
  public Iterator<Collection<SpanData>> iterator() {
    return fileSignalStorage.iterator();
  }
}
