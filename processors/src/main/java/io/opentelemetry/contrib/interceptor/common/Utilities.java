/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor.common;

import io.opentelemetry.contrib.interceptor.api.Interceptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Utilities {

  private Utilities() {}

  @SuppressWarnings("PreferredInterfaceType")
  public static <T> Collection<T> interceptAll(Collection<T> items, Interceptor<T> interceptor) {
    List<T> result = new ArrayList<>();

    for (T item : items) {
      T intercepted = interceptor.intercept(item);
      if (intercepted != null) {
        result.add(intercepted);
      }
    }

    return result;
  }
}
