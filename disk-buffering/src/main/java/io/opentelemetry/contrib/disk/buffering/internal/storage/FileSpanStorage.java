/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

/** Default storage implementation where items are stored in multiple protobuf files. */
public class FileSpanStorage implements SignalStorage.Span {

  @Override
  public CompletableFuture<WriteResult> write(Collection<SpanData> items) {
    throw new UnsupportedOperationException("For next PR");
  }

  @Override
  public CompletableFuture<WriteResult> clear() {
    throw new UnsupportedOperationException("For next PR");
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("For next PR");
  }

  @Nonnull
  @Override
  public Iterator<Collection<SpanData>> iterator() {
    throw new UnsupportedOperationException("For next PR");
  }
}
