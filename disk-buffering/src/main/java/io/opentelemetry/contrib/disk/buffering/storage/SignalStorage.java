package io.opentelemetry.contrib.disk.buffering.storage;

import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface SignalStorage<T> extends Iterable<Collection<T>>, Closeable {
  CompletableFuture<WriteResult> write(Collection<T> items);

  interface Span extends SignalStorage<SpanData> {}

  interface LogRecord extends SignalStorage<LogRecordData> {}

  interface Metric extends SignalStorage<MetricData> {}
}
