/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

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

  public SpanToDiskExporter(ToDiskExporter<SpanData> delegate) {
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
