/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * A filter that evaluates individual spans to determine if their containing trace's spans (within
 * the same export batch) should be exported. Used by {@link FilteringSpanExporter} to make per-span
 * keep/drop decisions.
 *
 * <p>If any {@code SpanFilter} returns {@code true} for any span in a batch, all spans sharing that
 * trace ID within the same batch are exported.
 */
public interface SpanFilter {

  /**
   * Evaluates whether the given span is interesting enough to keep its trace's spans in the batch.
   *
   * @param spanData the span to evaluate
   * @return {@code true} if this span should cause its trace's batch-colocated spans to be exported
   */
  boolean shouldKeep(SpanData spanData);
}
