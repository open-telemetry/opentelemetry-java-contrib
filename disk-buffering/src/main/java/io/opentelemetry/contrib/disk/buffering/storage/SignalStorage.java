package io.opentelemetry.contrib.disk.buffering.storage;

import io.opentelemetry.contrib.disk.buffering.storage.operations.ReadOperation;
import io.opentelemetry.contrib.disk.buffering.storage.operations.WriteOperation;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface SignalStorage<T> extends Closeable {
  CompletableFuture<WriteOperation> write(Collection<T> items);

  CompletableFuture<ReadOperation<T>> read();
}
