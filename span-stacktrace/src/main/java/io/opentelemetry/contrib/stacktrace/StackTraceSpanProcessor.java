/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.contrib.stacktrace.util.AbstractSimpleChainingSpanProcessor;
import io.opentelemetry.contrib.stacktrace.util.MutableSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StackTraceSpanProcessor extends AbstractSimpleChainingSpanProcessor {

  // TODO : remove this once semconv 1.24.0 is available
  static final AttributeKey<String> SPAN_STACKTRACE = AttributeKey.stringKey("code.stacktrace");

  private static final Logger logger = Logger.getLogger(StackTraceSpanProcessor.class.getName());

  private final long minSpanDurationNanos;

  private final Predicate<ReadableSpan> filterPredicate;

  /**
   * @param next next span processor to invoke
   * @param minSpanDurationNanos minimum span duration in ns for stacktrace capture
   * @param filterPredicate extra filter function to exclude spans if needed
   */
  public StackTraceSpanProcessor(
      SpanProcessor next, long minSpanDurationNanos, Predicate<ReadableSpan> filterPredicate) {
    super(next);
    this.minSpanDurationNanos = minSpanDurationNanos;
    this.filterPredicate = filterPredicate;
    logger.log(
        Level.FINE,
        "Stack traces will be added to spans with a minimum duration of {0} nanos",
        minSpanDurationNanos);
  }

  @Override
  protected boolean requiresStart() {
    return false;
  }

  @Override
  protected boolean requiresEnd() {
    return true;
  }

  @Override
  protected ReadableSpan doOnEnd(ReadableSpan span) {
    if (span.getLatencyNanos() < minSpanDurationNanos) {
      return span;
    }
    if (span.getAttribute(SPAN_STACKTRACE) != null) {
      // Span already has a stacktrace, do not override
      return span;
    }
    if (!filterPredicate.test(span)) {
      return span;
    }
    MutableSpan mutableSpan = MutableSpan.makeMutable(span);

    String stacktrace = generateSpanEndStacktrace();
    mutableSpan.setAttribute(SPAN_STACKTRACE, stacktrace);
    return mutableSpan;
  }

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
