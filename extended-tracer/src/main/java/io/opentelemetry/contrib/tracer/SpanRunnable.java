/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.tracer;

/**
 * A utility interface representing a {@link Runnable} that may throw.
 *
 * @param <E> Thrown exception type.
 */
@FunctionalInterface
public interface SpanRunnable<E extends Throwable> {
  void doInSpan() throws E;
}
