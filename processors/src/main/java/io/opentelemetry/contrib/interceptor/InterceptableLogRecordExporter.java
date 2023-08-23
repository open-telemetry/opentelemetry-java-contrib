/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor;

import io.opentelemetry.contrib.interceptor.common.Interceptable;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collection;

/** Intercepts logs before delegating them to the real exporter. */
public class InterceptableLogRecordExporter extends Interceptable<LogRecordData>
    implements LogRecordExporter {
  private final LogRecordExporter delegate;

  public InterceptableLogRecordExporter(LogRecordExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    return delegate.export(interceptAll(logs));
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
