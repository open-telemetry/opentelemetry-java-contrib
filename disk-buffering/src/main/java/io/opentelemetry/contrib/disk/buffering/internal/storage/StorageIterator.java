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

public final class StorageIterator<T> implements Iterator<Collection<T>> {
  private final Storage<T> storage;
  private final SignalDeserializer<T> deserializer;
  private final Logger logger = Logger.getLogger(StorageIterator.class.getName());

  @GuardedBy("this")
  @Nullable
  private ReadableResult<T> currentResult;

  @GuardedBy("this")
  private boolean currentResultConsumed = false;

  public StorageIterator(Storage<T> storage, SignalDeserializer<T> deserializer) {
    this.storage = storage;
    this.deserializer = deserializer;
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
    if (findNext()) {
      currentResultConsumed = true;
      return Objects.requireNonNull(currentResult).getContent();
    }
    return null;
  }

  @Override
  public synchronized void remove() {
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
        currentResult.delete();
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
