/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Collection;

/**
 * This class implements a SpanExporter that delegates to an instance of {@code
 * ToDiskExporter<SpanData>}.
 */
public class SpanToDiskExporter implements SpanExporter {

  private final ToDiskExporter<SpanData> delegate;

  /**
   * Creates a new SpanToDiskExporter that will buffer Span telemetry on disk storage.
   *
   * @param delegate - The SpanExporter to delegate to if disk writing fails.
   * @param config - The StorageConfiguration that specifies how storage is managed.
   * @return A new SpanToDiskExporter instance.
   * @throws IOException if the delegate ToDiskExporter could not be created.
   */
  @Deprecated
  public static SpanToDiskExporter create(SpanExporter delegate, StorageConfiguration config)
      throws IOException {
    ToDiskExporter<SpanData> toDisk =
        ToDiskExporter.<SpanData>builder()
            .setFolderName(SignalTypes.spans.name())
            .setStorageConfiguration(config)
            .setSerializer(SignalSerializer.ofSpans())
            .setExportFunction(delegate::export)
            .build();
    return new SpanToDiskExporter(toDisk);
  }

  /**
   * Creates a new SpanToDiskExporter that will buffer Span telemetry on disk storage.
   *
   * @param delegate - The SpanExporter to delegate to if disk writing fails.
   * @return A new SpanToDiskExporter instance.
   * @throws IOException if the delegate ToDiskExporter could not be created.
   */
  public static SpanToDiskExporter create(SpanExporter delegate, Storage storage)
      throws IOException {
    ToDiskExporter<SpanData> toDisk =
        ToDiskExporter.<SpanData>builder(storage)
            .setSerializer(SignalSerializer.ofSpans())
            .setExportFunction(delegate::export)
            .build();
    return new SpanToDiskExporter(toDisk);
  }

  // Visible for testing
  SpanToDiskExporter(ToDiskExporter<SpanData> delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return delegate.export(spans);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      delegate.shutdown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    }
    return CompletableResultCode.ofSuccess();
  }
}
