/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FilteringSpanExporterTest {

  private static final Duration SPAN_THRESHOLD = Duration.ofSeconds(2);
  private static final Duration TRACE_THRESHOLD = Duration.ofSeconds(10);

  private SpanExporter delegate;
  private FilteringSpanExporter exporter;

  @BeforeEach
  void setUp() {
    delegate = mock(SpanExporter.class);
    when(delegate.export(anyCollection())).thenReturn(CompletableResultCode.ofSuccess());
    exporter =
        new FilteringSpanExporter(
            delegate,
            Arrays.asList(new ErrorSpanFilter(), new DurationSpanFilter(SPAN_THRESHOLD)),
            Collections.singletonList(new TraceDurationFilter(TRACE_THRESHOLD)));
  }

  // --- Trace grouping: if one span is interesting, all siblings are kept ---

  @Test
  void uninterestingSpansAreDropped() {
    SpanData fastSpan = createSpan("trace-1", "span-1", StatusCode.OK, 0, 500);

    CompletableResultCode result = exporter.export(Collections.singletonList(fastSpan));

    assertThat(result.isSuccess()).isTrue();
    verify(delegate, never()).export(anyCollection());
  }

  @Test
  void traceGrouping_interestingSpanKeepsSiblings() {
    String traceId = "trace-1";
    SpanData slowSpan = createSpan(traceId, "span-1", StatusCode.OK, 0, 3000);
    SpanData fastSibling = createSpan(traceId, "span-2", StatusCode.OK, 0, 100);

    exporter.export(Arrays.asList(slowSpan, fastSibling));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(delegate).export(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(slowSpan, fastSibling);
  }

  @Test
  void mixedTraces_onlyInterestingTracesKept() {
    SpanData slowSpan = createSpan("trace-slow", "span-1", StatusCode.OK, 0, 3000);
    SpanData slowSibling = createSpan("trace-slow", "span-2", StatusCode.OK, 0, 200);
    SpanData fastSpan = createSpan("trace-fast", "span-3", StatusCode.OK, 0, 500);

    exporter.export(Arrays.asList(slowSpan, slowSibling, fastSpan));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(delegate).export(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(slowSpan, slowSibling);
  }

  @Test
  void traceKeptByTraceDurationFilter() {
    SpanData a1 = createSpan("trace-a", "span-1", StatusCode.OK, 0, 200);
    SpanData a2 = createSpan("trace-a", "span-2", StatusCode.OK, 14800, 200);
    SpanData b1 = createSpan("trace-b", "span-3", StatusCode.OK, 0, 300);

    exporter.export(Arrays.asList(a1, a2, b1));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(delegate).export(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(a1, a2);
  }

  // --- Composability: different filter combinations ---

  @Test
  void onlySpanFilters_noTraceFilters() {
    FilteringSpanExporter spanOnly =
        new FilteringSpanExporter(
            delegate,
            Collections.singletonList(new ErrorSpanFilter()),
            Collections.<TraceFilter>emptyList());

    SpanData errorSpan = createSpan("trace-1", "span-1", StatusCode.ERROR, 0, 100);
    SpanData fastSpan = createSpan("trace-2", "span-2", StatusCode.OK, 0, 100);

    spanOnly.export(Arrays.asList(errorSpan, fastSpan));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(delegate).export(captor.capture());
    assertThat(captor.getValue()).containsExactly(errorSpan);
  }

  @Test
  void onlyTraceFilters_noSpanFilters() {
    FilteringSpanExporter traceOnly =
        new FilteringSpanExporter(
            delegate,
            Collections.<SpanFilter>emptyList(),
            Collections.singletonList(new TraceDurationFilter(TRACE_THRESHOLD)));

    SpanData earlySpan = createSpan("trace-1", "span-1", StatusCode.OK, 0, 500);
    SpanData lateSpan = createSpan("trace-1", "span-2", StatusCode.OK, 11500, 500);
    SpanData errorSpan = createSpan("trace-2", "span-3", StatusCode.ERROR, 0, 100);

    traceOnly.export(Arrays.asList(earlySpan, lateSpan, errorSpan));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(delegate).export(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(earlySpan, lateSpan);
  }

  @Test
  void customSpanFilter() {
    SpanFilter nameFilter = span -> span.getName().contains("important");
    FilteringSpanExporter custom =
        new FilteringSpanExporter(
            delegate, Collections.singletonList(nameFilter), Collections.<TraceFilter>emptyList());

    SpanData important = createNamedSpan("trace-1", "important-operation", StatusCode.OK, 0, 100);
    SpanData boring = createNamedSpan("trace-2", "boring-operation", StatusCode.OK, 0, 100);

    custom.export(Arrays.asList(important, boring));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(delegate).export(captor.capture());
    assertThat(captor.getValue()).containsExactly(important);
  }

  // --- Metrics ---

  @Test
  void droppedSpanMetricIsEmittedWhenMeterProvided() {
    Meter meter = mock(Meter.class);
    LongCounterBuilder counterBuilder = mock(LongCounterBuilder.class);
    LongCounter counter = mock(LongCounter.class);
    when(meter.counterBuilder("otel.contrib.processor.span.filtered")).thenReturn(counterBuilder);
    when(counterBuilder.setDescription("Number of spans dropped by the filtering span exporter"))
        .thenReturn(counterBuilder);
    when(counterBuilder.setUnit("{span}")).thenReturn(counterBuilder);
    when(counterBuilder.build()).thenReturn(counter);

    FilteringSpanExporter exporterWithMetrics =
        new FilteringSpanExporter(
            delegate,
            Collections.singletonList(new DurationSpanFilter(SPAN_THRESHOLD)),
            Collections.<TraceFilter>emptyList(),
            meter);

    SpanData fastSpan = createSpan("trace-1", "span-1", StatusCode.OK, 0, 500);
    exporterWithMetrics.export(Collections.singletonList(fastSpan));

    verify(counter).add(eq(1L), any());
  }

  @Test
  void noMetricEmittedWhenNoSpansDropped() {
    Meter meter = mock(Meter.class);
    LongCounterBuilder counterBuilder = mock(LongCounterBuilder.class);
    LongCounter counter = mock(LongCounter.class);
    when(meter.counterBuilder("otel.contrib.processor.span.filtered")).thenReturn(counterBuilder);
    when(counterBuilder.setDescription("Number of spans dropped by the filtering span exporter"))
        .thenReturn(counterBuilder);
    when(counterBuilder.setUnit("{span}")).thenReturn(counterBuilder);
    when(counterBuilder.build()).thenReturn(counter);

    FilteringSpanExporter exporterWithMetrics =
        new FilteringSpanExporter(
            delegate,
            Collections.singletonList(new DurationSpanFilter(SPAN_THRESHOLD)),
            Collections.<TraceFilter>emptyList(),
            meter);

    SpanData slowSpan = createSpan("trace-1", "span-1", StatusCode.OK, 0, 3000);
    exporterWithMetrics.export(Collections.singletonList(slowSpan));

    verify(counter, never()).add(anyLong(), any());
  }

  // --- Delegation ---

  @Test
  void flushDelegatesToWrapped() {
    when(delegate.flush()).thenReturn(CompletableResultCode.ofSuccess());

    CompletableResultCode result = exporter.flush();

    assertThat(result.isSuccess()).isTrue();
    verify(delegate).flush();
  }

  @Test
  void shutdownDelegatesToWrapped() {
    when(delegate.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

    CompletableResultCode result = exporter.shutdown();

    assertThat(result.isSuccess()).isTrue();
    verify(delegate).shutdown();
  }

  @Test
  void emptyBatchReturnsSuccess() {
    CompletableResultCode result = exporter.export(Collections.emptyList());

    assertThat(result.isSuccess()).isTrue();
    verify(delegate, never()).export(anyCollection());
  }

  // --- Helpers ---

  private static SpanData createSpan(
      String traceId, String spanId, StatusCode statusCode, long startOffsetMs, long durationMs) {
    return createSpanNanos(
        traceId,
        spanId,
        statusCode,
        TimeUnit.MILLISECONDS.toNanos(startOffsetMs),
        TimeUnit.MILLISECONDS.toNanos(durationMs));
  }

  private static SpanData createNamedSpan(
      String traceId, String name, StatusCode statusCode, long startOffsetMs, long durationMs) {
    SpanData span =
        createSpanNanos(
            traceId,
            "span-id",
            statusCode,
            TimeUnit.MILLISECONDS.toNanos(startOffsetMs),
            TimeUnit.MILLISECONDS.toNanos(durationMs));
    when(span.getName()).thenReturn(name);
    return span;
  }

  private static SpanData createSpanNanos(
      String traceId,
      String spanId,
      StatusCode statusCode,
      long startOffsetNanos,
      long durationNanos) {
    SpanData span = mock(SpanData.class);
    SpanContext context = mock(SpanContext.class);
    StatusData status = mock(StatusData.class);

    when(context.getTraceId()).thenReturn(traceId);
    when(context.getSpanId()).thenReturn(spanId);
    when(span.getSpanContext()).thenReturn(context);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(span.getStatus()).thenReturn(status);

    long baseNanos = TimeUnit.MILLISECONDS.toNanos(1_000_000_000L);
    long startNanos = baseNanos + startOffsetNanos;
    when(span.getStartEpochNanos()).thenReturn(startNanos);
    when(span.getEndEpochNanos()).thenReturn(startNanos + durationNanos);

    return span;
  }
}
