/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;

/**
 * A filter that evaluates all spans belonging to a single trace within an export batch to determine
 * if those spans should be exported. Used by {@link FilteringSpanExporter} for decisions that
 * require batch-level context (e.g., overall trace wall-clock duration).
 *
 * <p>If any {@code TraceFilter} returns {@code true} for a trace, all spans sharing that trace ID
 * within the same batch are exported.
 */
public interface TraceFilter {

  /**
   * Evaluates whether the given trace (represented as all spans sharing a trace ID within the
   * current batch) should be exported.
   *
   * @param traceId the trace ID
   * @param spans all spans in the current batch belonging to this trace
   * @return {@code true} if this trace should be exported
   */
  boolean shouldKeep(String traceId, Collection<SpanData> spans);
}
