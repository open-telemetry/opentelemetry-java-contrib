/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback;
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import io.opentelemetry.contrib.disk.buffering.storage.result.WriteResult;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class SignalStorageExporterTest {
  @Mock private ExporterCallback<SpanData> callback;

  @Test
  void verifyExportToStorage_success() {
    SignalStorage.Span storage = new TestSpanStorage();
    SignalStorageExporter<SpanData> storageExporter =
        new SignalStorageExporter<>(storage, callback, Duration.ofSeconds(1));
    SpanData item1 = mock();
    SpanData item2 = mock();
    SpanData item3 = mock();

    List<SpanData> items = Arrays.asList(item1, item2);
    CompletableResultCode resultCode = storageExporter.exportToStorage(items);

    assertThat(resultCode.isSuccess()).isTrue();
    verify(callback).onExportSuccess(items);
    verifyNoMoreInteractions(callback);

    // Adding more items
    clearInvocations(callback);
    List<SpanData> items2 = Collections.singletonList(item3);
    resultCode = storageExporter.exportToStorage(items2);

    assertThat(resultCode.isSuccess()).isTrue();
    verify(callback).onExportSuccess(items2);
    verifyNoMoreInteractions(callback);

    // Checking items
    List<SpanData> storedItems = new ArrayList<>();
    for (Collection<SpanData> collection : storage) {
      storedItems.addAll(collection);
    }
    assertThat(storedItems).containsExactly(item1, item2, item3);
  }

  @Test
  void verifyExportToStorage_failure() {
    SignalStorage.Span storage = mock();
    SignalStorageExporter<SpanData> storageExporter =
        new SignalStorageExporter<>(storage, callback, Duration.ofSeconds(1));
    SpanData item1 = mock();

    // Without exception
    when(storage.write(anyCollection()))
        .thenReturn(CompletableFuture.completedFuture(WriteResult.error(null)));

    List<SpanData> items = Collections.singletonList(item1);
    CompletableResultCode resultCode = storageExporter.exportToStorage(items);

    assertThat(resultCode.isSuccess()).isFalse();
    assertThat(resultCode.getFailureThrowable()).isNull();
    verify(callback).onExportError(items, null);
    verifyNoMoreInteractions(callback);

    // With exception
    clearInvocations(callback);
    Exception exception = new Exception();
    when(storage.write(anyCollection()))
        .thenReturn(CompletableFuture.completedFuture(WriteResult.error(exception)));

    resultCode = storageExporter.exportToStorage(items);

    assertThat(resultCode.isSuccess()).isFalse();
    assertThat(resultCode.getFailureThrowable()).isEqualTo(exception);
    verify(callback).onExportError(items, exception);
    verifyNoMoreInteractions(callback);
  }

  private static class TestSpanStorage implements SignalStorage.Span {
    private final List<Collection<SpanData>> storedItems = new ArrayList<>();

    @Override
    public CompletableFuture<WriteResult> write(Collection<SpanData> items) {
      storedItems.add(items);
      return getSuccessfulFuture();
    }

    @Override
    public CompletableFuture<WriteResult> clear() {
      storedItems.clear();
      return getSuccessfulFuture();
    }

    @Override
    public void close() {}

    @Nonnull
    @Override
    public Iterator<Collection<SpanData>> iterator() {
      return storedItems.iterator();
    }

    @Nonnull
    private static CompletableFuture<WriteResult> getSuccessfulFuture() {
      return CompletableFuture.completedFuture(WriteResult.successful());
    }
  }
}
