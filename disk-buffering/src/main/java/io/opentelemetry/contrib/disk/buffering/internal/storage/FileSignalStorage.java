/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Default storage implementation where items are stored in multiple protobuf files. */
public final class FileSignalStorage<T> implements SignalStorage<T> {
  private final Storage<T> storage;
  private final SignalSerializer<T> serializer;
  private final SignalDeserializer<T> deserializer;
  private final Logger logger = Logger.getLogger(FileSignalStorage.class.getName());
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final Object iteratorLock = new Object();

  @GuardedBy("iteratorLock")
  @Nullable
  private Iterator<Collection<T>> iterator;

  public FileSignalStorage(
      Storage<T> storage, SignalSerializer<T> serializer, SignalDeserializer<T> deserializer) {
    this.storage = storage;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  @Override
  public CompletableFuture<WriteResult> write(Collection<T> items) {
    logger.finer("Intercepting batch.");
    try {
      serializer.initialize(items);
      if (storage.write(serializer)) {
        return CompletableFuture.completedFuture(WriteResult.successful());
      }
      logger.fine("Could not store batch in disk.");
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
  public Iterator<Collection<T>> iterator() {
    synchronized (iteratorLock) {
      if (iterator == null) {
        iterator = new StorageIterator<>(storage, deserializer);
      }
      return iterator;
    }
  }
}
