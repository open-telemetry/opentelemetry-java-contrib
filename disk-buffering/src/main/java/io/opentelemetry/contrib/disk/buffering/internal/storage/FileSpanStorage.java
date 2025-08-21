/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/** Default storage implementation where items are stored in multiple protobuf files. */
public final class FileSpanStorage implements SignalStorage.Span {
  private final Storage storage;
  private final SignalSerializer<SpanData> serializer;
  private final Logger logger = Logger.getLogger(FileSpanStorage.class.getName());
  private final AtomicBoolean isClosed = new AtomicBoolean(false);

  public static FileSpanStorage create(File destinationDir, StorageConfiguration configuration) {
    return create(destinationDir, configuration, Clock.getDefault());
  }

  public static FileSpanStorage create(
      File destinationDir, StorageConfiguration configuration, Clock clock) {
    FolderManager folderManager = FolderManager.create(destinationDir, configuration, clock);
    return new FileSpanStorage(
        new Storage(folderManager, configuration.isDebugEnabled()), SignalSerializer.ofSpans());
  }

  FileSpanStorage(Storage storage, SignalSerializer<SpanData> serializer) {
    this.storage = storage;
    this.serializer = serializer;
  }

  @Override
  public CompletableFuture<WriteResult> write(Collection<SpanData> items) {
    logger.finer("Intercepting batch.");
    try {
      serializer.initialize(items);
      if (storage.write(serializer)) {
        return CompletableFuture.completedFuture(WriteResult.successful());
      }
      logger.info("Could not store batch in disk.");
      return CompletableFuture.completedFuture(WriteResult.error(null));
    } catch (IOException e) {
      logger.log(
          Level.WARNING,
          "An unexpected error happened while attempting to write the data in disk.",
          e);
      return CompletableFuture.completedFuture(WriteResult.error(e));
    } finally {
      serializer.reset();
    }
  }

  @Override
  public CompletableFuture<WriteResult> clear() {
    try {
      storage.clear();
      return CompletableFuture.completedFuture(WriteResult.successful());
    } catch (IOException e) {
      return CompletableFuture.completedFuture(WriteResult.error(e));
    }
  }

  @Override
  public void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      storage.close();
    }
  }

  @Nonnull
  @Override
  public Iterator<Collection<SpanData>> iterator() {
    throw new UnsupportedOperationException("For next PR");
  }
}
