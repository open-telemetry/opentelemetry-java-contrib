/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage;

import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Allows writing and iterating over written signal items.
 *
 * @param <T> The type of signal data supported.
 */
public interface SignalStorage<T> extends Iterable<Collection<T>>, Closeable {

  /**
   * Stores signal items.
   *
   * @param items The items to be stored.
   * @return A future with {@link WriteResult}.
   */
  CompletableFuture<WriteResult> write(Collection<T> items);

  /**
   * Removes all the previously stored items.
   *
   * @return A future with {@link WriteResult}.
   */
  CompletableFuture<WriteResult> clear();

  /**
   * Abstraction for Spans. Implementations should use this instead of {@link SignalStorage}
   * directly.
   */
  interface Span extends SignalStorage<SpanData> {}

  /**
   * Abstraction for Logs. Implementations should use this instead of {@link SignalStorage}
   * directly.
   */
  interface LogRecord extends SignalStorage<LogRecordData> {}

  /**
   * Abstraction for Metrics. Implementations should use this instead of {@link SignalStorage}
   * directly.
   */
  interface Metric extends SignalStorage<MetricData> {}
}
