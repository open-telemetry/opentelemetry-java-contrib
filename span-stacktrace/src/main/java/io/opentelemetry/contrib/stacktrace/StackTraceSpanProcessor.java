/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.internal.ExtendedSpanProcessor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;

public class StackTraceSpanProcessor implements ExtendedSpanProcessor {

  // inlined incubating attribute to prevent direct dependency on incubating semconv
  private static final AttributeKey<String> SPAN_STACKTRACE =
      AttributeKey.stringKey("code.stacktrace");

  private final long minSpanDurationNanos;

  private final Predicate<ReadableSpan> filterPredicate;

  /**
   * @param minSpanDurationNanos minimum span duration in ns for stacktrace capture
   * @param filterPredicate extra filter function to exclude spans if needed
   */
  public StackTraceSpanProcessor(
      long minSpanDurationNanos, Predicate<ReadableSpan> filterPredicate) {
    this.minSpanDurationNanos = minSpanDurationNanos;
    this.filterPredicate = filterPredicate;
  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {}

  @Override
  public boolean isOnEndingRequired() {
    return true;
  }

  @Override
  public void onEnding(ReadWriteSpan span) {
    if (minSpanDurationNanos < 0 || span.getLatencyNanos() < minSpanDurationNanos) {
      return;
    }
    if (span.getAttribute(SPAN_STACKTRACE) != null) {
      // Span already has a stacktrace, do not override
      return;
    }
    if (!filterPredicate.test(span)) {
      return;
    }
    span.setAttribute(SPAN_STACKTRACE, generateSpanEndStacktrace());
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {}

  private static String generateSpanEndStacktrace() {
    Throwable exception = new Throwable();
    StringWriter stringWriter = new StringWriter();
    try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
      exception.printStackTrace(printWriter);
    }
    return removeInternalFrames(stringWriter.toString());
  }

  private static String removeInternalFrames(String stackTrace) {
    String lastInternal = "at io.opentelemetry.sdk.trace.SdkSpan.end";

    int idx = stackTrace.lastIndexOf(lastInternal);
    if (idx == -1) {
      // should usually not happen, this means that the span processor was called from somewhere
      // else
      return stackTrace;
    }
    int nextNewLine = stackTrace.indexOf('\n', idx);
    if (nextNewLine == -1) {
      nextNewLine = stackTrace.length() - 1;
    }
    return stackTrace.substring(nextNewLine + 1);
  }
}
