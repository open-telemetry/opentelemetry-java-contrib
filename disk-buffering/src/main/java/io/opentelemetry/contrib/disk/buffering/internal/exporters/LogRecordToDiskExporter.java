/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

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

  public LogRecordToDiskExporter(ToDiskExporter<LogRecordData> delegate) {
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
