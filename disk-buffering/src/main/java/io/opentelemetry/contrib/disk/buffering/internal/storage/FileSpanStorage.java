package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class FileSpanStorage implements SignalStorage.Span {

  @Override
  public CompletableFuture<WriteResult> write(Collection<SpanData> items) {
    return null;
  }

  @Override
  public CompletableFuture<WriteResult> clear() {
    return null;
  }

  @Override
  public void close() throws IOException {}

  @Nonnull
  @Override
  public Iterator<Collection<SpanData>> iterator() {
    return null;
  }
}
