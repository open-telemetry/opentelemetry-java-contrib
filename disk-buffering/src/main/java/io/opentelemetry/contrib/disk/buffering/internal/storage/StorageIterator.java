/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

final class StorageIterator<T> implements Iterator<Collection<T>> {
  private final Storage<T> storage;
  private final SignalDeserializer<T> deserializer;
  private final boolean deleteOnIteration;
  private final Logger logger = Logger.getLogger(StorageIterator.class.getName());

  @GuardedBy("this")
  @Nullable
  private ReadableResult<T> currentResult;

  @GuardedBy("this")
  private boolean currentResultConsumed = false;

  @GuardedBy("this")
  private boolean removeAllowed = false;

  StorageIterator(
      Storage<T> storage, SignalDeserializer<T> deserializer, boolean deleteOnIteration) {
    this.storage = storage;
    this.deserializer = deserializer;
    this.deleteOnIteration = deleteOnIteration;
  }

  @Override
  public synchronized boolean hasNext() {
    if (storage.isClosed()) {
      return false;
    }
    return findNext();
  }

  @Override
  @Nullable
  public synchronized Collection<T> next() {
    if (storage.isClosed()) {
      return null;
    }
    if (findNext()) {
      currentResultConsumed = true;
      removeAllowed = true;
      return Objects.requireNonNull(currentResult).getContent();
    }
    return null;
  }

  @Override
  public synchronized void remove() {
    if (!removeAllowed) {
      throw new IllegalStateException("next() must be called before remove()");
    }
    removeAllowed = false;
    if (currentResult != null) {
      try {
        currentResult.delete();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Error deleting stored item", e);
      }
    }
  }

  private synchronized boolean findNext() {
    try {
      if (currentResult != null) {
        if (!currentResultConsumed) {
          return true;
        }
        if (deleteOnIteration) {
          currentResult.delete();
        }
        currentResult.close();
        currentResult = null;
      }

      currentResultConsumed = false;
      ReadableResult<T> result = storage.readNext(deserializer);
      if (result != null) {
        currentResult = result;
        return true;
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error reading from storage", e);
    }
    return false;
  }
}
