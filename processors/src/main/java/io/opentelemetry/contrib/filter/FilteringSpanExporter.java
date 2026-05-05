/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A {@link SpanExporter} wrapper that filters spans before delegating to the underlying exporter.
 * Filtering operates at the trace level within each export batch: if any filter matches, all spans
 * sharing that trace ID <em>in that batch</em> are exported together.
 *
 * <p>Two types of filters are supported:
 *
 * <ul>
 *   <li>{@link SpanFilter} - evaluates individual spans (e.g., error status, slow duration)
 *   <li>{@link TraceFilter} - evaluates all spans belonging to a trace within the batch (e.g.,
 *       overall trace wall-clock duration)
 * </ul>
 *
 * <p>A trace's spans are kept if any {@code SpanFilter} matches any span in the batch, OR any
 * {@code TraceFilter} matches the trace's span group within the batch.
 *
 * <p><strong>Important:</strong> Filtering decisions are scoped to a single {@link
 * #export(Collection)} call. Spans from the same trace that arrive in different batches are
 * evaluated independently, so a trace split across batches may be partially exported.
 */
public final class FilteringSpanExporter implements SpanExporter {

  private static final AttributeKey<String> REASON_KEY = AttributeKey.stringKey("reason");

  private final SpanExporter delegate;
  private final List<SpanFilter> spanFilters;
  private final List<TraceFilter> traceFilters;
  @Nullable private final LongCounter droppedSpansCounter;

  /**
   * Creates a new {@code FilteringSpanExporter}.
   *
   * @param delegate the exporter to delegate to for spans that pass filtering
   * @param spanFilters per-span filters; a trace's spans in the batch are kept if any filter
   *     matches
   * @param traceFilters batch-level filters; a trace's spans in the batch are kept if any filter
   *     matches
   * @param meter optional {@link Meter} for emitting dropped-span metrics; pass {@code null} to
   *     disable metrics
   */
  public FilteringSpanExporter(
      SpanExporter delegate,
      List<SpanFilter> spanFilters,
      List<TraceFilter> traceFilters,
      @Nullable Meter meter) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    Objects.requireNonNull(spanFilters, "spanFilters");
    Objects.requireNonNull(traceFilters, "traceFilters");
    this.spanFilters = Collections.unmodifiableList(new ArrayList<>(spanFilters));
    this.traceFilters = Collections.unmodifiableList(new ArrayList<>(traceFilters));
    if (meter != null) {
      this.droppedSpansCounter =
          meter
              .counterBuilder("otel.contrib.processor.span.filtered")
              .setDescription("Number of spans dropped by the filtering span exporter")
              .setUnit("{span}")
              .build();
    } else {
      this.droppedSpansCounter = null;
    }
  }

  /**
   * Creates a new {@code FilteringSpanExporter} without metrics.
   *
   * @param delegate the exporter to delegate to for spans that pass filtering
   * @param spanFilters per-span filters
   * @param traceFilters batch-level filters
   */
  public FilteringSpanExporter(
      SpanExporter delegate, List<SpanFilter> spanFilters, List<TraceFilter> traceFilters) {
    this(delegate, spanFilters, traceFilters, null);
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    // Group spans by trace ID and evaluate span-level filters in a single pass
    Set<String> interestingTraceIds = new HashSet<>();
    Map<String, List<SpanData>> spansByTrace = new HashMap<>();

    for (SpanData span : spans) {
      String traceId = span.getSpanContext().getTraceId();

      List<SpanData> traceSpans = spansByTrace.get(traceId);
      if (traceSpans == null) {
        traceSpans = new ArrayList<>();
        spansByTrace.put(traceId, traceSpans);
      }
      traceSpans.add(span);

      // Check span-level filters
      if (!interestingTraceIds.contains(traceId)) {
        for (SpanFilter filter : spanFilters) {
          if (filter.shouldKeep(span)) {
            interestingTraceIds.add(traceId);
            break;
          }
        }
      }
    }

    // Evaluate trace-level filters
    if (!traceFilters.isEmpty()) {
      for (Map.Entry<String, List<SpanData>> entry : spansByTrace.entrySet()) {
        String traceId = entry.getKey();
        if (!interestingTraceIds.contains(traceId)) {
          for (TraceFilter filter : traceFilters) {
            if (filter.shouldKeep(traceId, entry.getValue())) {
              interestingTraceIds.add(traceId);
              break;
            }
          }
        }
      }
    }

    // Collect filtered spans
    List<SpanData> filtered = new ArrayList<>();
    long droppedCount = 0;

    for (SpanData span : spans) {
      if (interestingTraceIds.contains(span.getSpanContext().getTraceId())) {
        filtered.add(span);
      } else {
        droppedCount++;
      }
    }

    if (droppedSpansCounter != null && droppedCount > 0) {
      droppedSpansCounter.add(droppedCount, Attributes.of(REASON_KEY, "not_interesting"));
    }

    if (filtered.isEmpty()) {
      return CompletableResultCode.ofSuccess();
    }

    return delegate.export(filtered);
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
