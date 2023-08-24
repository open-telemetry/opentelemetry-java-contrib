/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor;

import static io.opentelemetry.contrib.interceptor.common.Utilities.interceptAll;

import io.opentelemetry.contrib.interceptor.api.Interceptor;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collection;

/** Intercepts logs before delegating them to the real exporter. */
public class InterceptableLogRecordExporter implements LogRecordExporter {
  private final LogRecordExporter delegate;
  private final Interceptor<LogRecordData> interceptor;

  public InterceptableLogRecordExporter(
      LogRecordExporter delegate, Interceptor<LogRecordData> interceptor) {
    this.delegate = delegate;
    this.interceptor = interceptor;
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    return delegate.export(interceptAll(logs, interceptor));
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
