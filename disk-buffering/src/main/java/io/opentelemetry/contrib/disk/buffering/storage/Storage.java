package io.opentelemetry.contrib.disk.buffering.storage;

import io.opentelemetry.contrib.disk.buffering.storage.operations.ReadOperation;
import io.opentelemetry.contrib.disk.buffering.storage.operations.WriteOperation;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface Storage<T> extends Closeable {
  CompletableFuture<WriteOperation> write(Collection<T> items);

  CompletableFuture<ReadOperation<T>> read();

  interface Span extends Storage<SpanData> {}

  interface LogRecord extends Storage<LogRecordData> {}

  interface Metric extends Storage<MetricData> {}
}
