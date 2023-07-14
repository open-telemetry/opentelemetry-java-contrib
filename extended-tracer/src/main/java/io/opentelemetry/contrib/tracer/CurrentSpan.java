/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.tracer;

import io.opentelemetry.api.trace.Span;

/** Utility class for the current span. */
public final class CurrentSpan {
  private CurrentSpan() {}

  /**
   * Marks the current span as error.
   *
   * @param description what went wrong
   * @param exception the exception that caused the error
   */
  public static void setSpanError(String description, Throwable exception) {
    Tracing.setSpanError(Span.current(), description, exception);
  }

  /**
   * Marks the current span as error.
   *
   * @param description what went wrong
   */
  public static void setSpanError(String description) {
    Tracing.setSpanError(Span.current(), description);
  }

  /**
   * Marks the current span as error.
   *
   * @param exception the exception that caused the error
   */
  public static void setSpanError(Throwable exception) {
    Tracing.setSpanError(Span.current(), exception);
  }
}
