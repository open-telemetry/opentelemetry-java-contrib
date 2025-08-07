package io.opentelemetry.contrib.disk.buffering.storage.operations;

import java.io.Closeable;
import java.util.Collection;

public interface ReadOperation<T> extends Closeable {
  Collection<T> getItems();

  void setStatus(Status status);

  enum Status {
    SUCCESS,
    FAILURE
  }
}
