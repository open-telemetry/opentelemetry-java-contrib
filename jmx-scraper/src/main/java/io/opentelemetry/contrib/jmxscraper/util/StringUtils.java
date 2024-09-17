/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.util;

import javax.annotation.Nullable;

public final class StringUtils {
  private StringUtils() {}

  /**
   * Determines if a String is null or without non-whitespace chars.
   *
   * @param s - {@link String} to evaluate
   * @return - if s is null or without non-whitespace chars.
   */
  public static boolean isBlank(@Nullable String s) {
    return (s == null) || s.trim().isEmpty();
  }
}
