/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor;

import io.opentelemetry.contrib.interceptor.common.Interceptable;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;

/** Intercepts spans before delegating them to the real exporter. */
public class InterceptableSpanExporter extends Interceptable<SpanData> implements SpanExporter {
  private final SpanExporter delegate;

  public InterceptableSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return delegate.export(interceptAll(spans));
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }
}
