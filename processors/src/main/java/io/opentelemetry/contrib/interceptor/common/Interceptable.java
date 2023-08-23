/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor.common;

import io.opentelemetry.contrib.interceptor.api.Interceptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Base class to reuse the code related to intercepting signals. */
public class Interceptable<T> {
  private final Set<Interceptor<T>> interceptors = new HashSet<>();

  public void addInterceptor(Interceptor<T> interceptor) {
    interceptors.add(interceptor);
  }

  protected Collection<T> interceptAll(Collection<T> items) {
    List<T> result = new ArrayList<>();

    for (T item : items) {
      T intercepted = intercept(item);
      if (intercepted != null) {
        result.add(intercepted);
      }
    }

    return result;
  }

  private T intercept(T item) {
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
