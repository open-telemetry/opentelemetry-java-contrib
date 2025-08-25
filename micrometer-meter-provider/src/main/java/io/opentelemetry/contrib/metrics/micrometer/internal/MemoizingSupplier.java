/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal;

import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Delegating implementation of {@link Supplier Supplier<T>} that ensures that the {@link
 * Supplier#get()} method is called at most once and the result is memoized for subsequent
 * invocations.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class MemoizingSupplier<T> implements Supplier<T> {
  private final Supplier<T> delegate;
  private volatile boolean initialized;
  @Nullable private volatile T cachedResult;

  public MemoizingSupplier(Supplier<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  @SuppressWarnings("NullAway")
  public T get() {
    T result = cachedResult;
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          result = delegate.get();
          cachedResult = result;
          initialized = true;
          return result;
        }
      }
    }
    return result;
  }
}
