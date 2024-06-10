/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class TemporaryProperties implements AutoCloseable {

  private final Map<String, String> originalValues = new HashMap<>();

  @CanIgnoreReturnValue
  public TemporaryProperties put(String key, @Nullable String value) {
    if (!originalValues.containsKey(key)) {
      originalValues.put(key, System.getProperty(key));
    }
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
    return this;
  }

  @Override
  public void close() {
    for (String key : originalValues.keySet()) {
      String value = originalValues.get(key);
      if (value == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, value);
      }
    }
  }
}
