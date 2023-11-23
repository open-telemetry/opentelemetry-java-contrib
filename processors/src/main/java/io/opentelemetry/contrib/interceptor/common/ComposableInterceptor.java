/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor.common;

import io.opentelemetry.contrib.interceptor.api.Interceptor;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

/** Allows to run an item through a list of interceptors in the order they were added. */
public final class ComposableInterceptor<T> implements Interceptor<T> {
  private final CopyOnWriteArrayList<Interceptor<T>> interceptors = new CopyOnWriteArrayList<>();

  public void add(Interceptor<T> interceptor) {
    interceptors.addIfAbsent(interceptor);
  }

  @Nullable
  @Override
  public T intercept(T item) {
    T intercepted = item;
    for (Interceptor<T> interceptor : interceptors) {
      intercepted = interceptor.intercept(intercepted);
      if (intercepted == null) {
        break;
      }
    }
    return intercepted;
  }
}
