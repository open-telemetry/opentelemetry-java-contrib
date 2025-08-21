/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.ToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.io.IOException;
import java.util.Collection;

/**
 * This class implements a {@link LogRecordExporter} that delegates to an instance of {@code
 * ToDiskExporter<LogRecordData>}.
 */
public class LogRecordToDiskExporter implements LogRecordExporter {
  private final ToDiskExporter<LogRecordData> delegate;

  /**
   * Creates a new LogRecordToDiskExporter that will buffer LogRecordData telemetry on disk storage.
   *
   * @param delegate - The LogRecordExporter to delegate to if disk writing fails.
   * @param storage - The Storage instance that specifies how storage is managed.
   * @return A new LogRecordToDiskExporter instance.
   */
  public static LogRecordToDiskExporter create(
      LogRecordExporter delegate, Storage<LogRecordData> storage) {
    ToDiskExporter<LogRecordData> toDisk =
        ToDiskExporter.builder(storage)
            .setSerializer(SignalSerializer.ofLogs())
            .setExportFunction(delegate::export)
            .build();
    return new LogRecordToDiskExporter(toDisk);
  }

  // Visible for testing
  LogRecordToDiskExporter(ToDiskExporter<LogRecordData> delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    return delegate.export(logs);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      delegate.shutdown();
      return CompletableResultCode.ofSuccess();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    }
  }
}
