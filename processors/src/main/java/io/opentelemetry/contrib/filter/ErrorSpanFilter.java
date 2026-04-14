/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * A {@link SpanFilter} that matches spans with {@link StatusCode#ERROR}, causing all
 * batch-colocated spans sharing the same trace ID to be exported.
 */
public final class ErrorSpanFilter implements SpanFilter {

  @Override
  public boolean shouldKeep(SpanData spanData) {
    return spanData.getStatus().getStatusCode() == StatusCode.ERROR;
  }
}
