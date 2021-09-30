/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

final class StringUtils {

  private StringUtils() {}

  /** Copy org.apache.commons.lang3.StringUtils#isBlank(java.lang.CharSequence) */
  public static boolean isBlank(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (Character.isWhitespace(str.charAt(i)) == false) {
        return false;
      }
    }
    return true;
  }

  /** Copy org.apache.commons.lang3.StringUtils#isNotBlank(java.lang.CharSequence) */
  public static boolean isNotBlank(String str) {
    return !isBlank(str);
  }

  /**
   * Copy org.apache.commons.lang3.StringUtils#defaultIfBlank(java.lang.CharSequence,
   * java.lang.CharSequence)
   */
  public static String defaultIfBlank(String str, String defaultStr) {
    return isBlank(str) ? defaultStr : str;
  }
}
