/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;

/** A {@link SpanFilter} that keeps traces containing any span with {@link StatusCode#ERROR}. */
public final class ErrorSpanFilter implements SpanFilter {

  @Override
  public boolean shouldKeep(SpanData spanData) {
    return spanData.getStatus().getStatusCode() == StatusCode.ERROR;
  }
}
