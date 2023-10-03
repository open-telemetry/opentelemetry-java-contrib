/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent2;

import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getInvalidRandomValue;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getMaxThreshold;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.isValidRandomValue;
import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.isValidThreshold;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

final class OtelTraceState {

  public static final String TRACE_STATE_KEY = "ot";

  private static final String SUBKEY_RANDOM_VALUE = "rv";
  private static final String SUBKEY_THRESHOLD = "th";
  private static final int TRACE_STATE_SIZE_LIMIT = 256;

  private long randomValue; // valid in the interval [0, MAX_RANDOM_VALUE]
  private long threshold; // valid in the interval [0, MAX_THRESHOLD]

  private final List<String> otherKeyValuePairs;

  private OtelTraceState(long randomValue, long threshold, List<String> otherKeyValuePairs) {
    this.randomValue = randomValue;
    this.threshold = threshold;
    this.otherKeyValuePairs = otherKeyValuePairs;
  }

  private OtelTraceState() {
    this(getInvalidRandomValue(), getInvalidThreshold(), Collections.emptyList());
  }

  public long getRandomValue() {
    return randomValue;
  }

  public long getThreshold() {
    return threshold;
  }

  public boolean hasValidRandomValue() {
    return isValidRandomValue(randomValue);
  }

  public boolean hasValidThreshold() {
    return isValidThreshold(threshold);
  }

  public void invalidateRandomValue() {
    randomValue = getInvalidRandomValue();
  }

  public void invalidateThreshold() {
    threshold = getInvalidThreshold();
  }

  /**
   * Sets a new th-value.
   *
   * <p>If the given th-value is invalid, the current th-value is invalidated.
   *
   * @param threshold the new th-value
   */
  public void setThreshold(long threshold) {
    if (isValidThreshold(threshold)) {
      this.threshold = threshold;
    } else {
      invalidateThreshold();
    }
  }

  /**
   * Sets a new rv-value.
   *
   * <p>If the given rv-value is invalid, the current rv-value is invalidated.
   *
   * @param randomValue the new rv-value
   */
  public void setRandomValue(long randomValue) {
    if (isValidRandomValue(randomValue)) {
      this.randomValue = randomValue;
    } else {
      invalidateRandomValue();
    }
  }

  /**
   * Returns a string representing this state.
   *
   * @return a string
   */
  public String serialize() {
    StringBuilder sb = new StringBuilder();
    if (hasValidThreshold() && threshold < getMaxThreshold()) {
      sb.append(SUBKEY_THRESHOLD).append(':');
      appendThreshold(sb, threshold);
    }
    if (hasValidRandomValue()) {
      if (sb.length() > 0) {
        sb.append(';');
      }
      sb.append(SUBKEY_RANDOM_VALUE).append(':');
      appendRandomValue(sb, randomValue);
    }
    for (String pair : otherKeyValuePairs) {
      int ex = sb.length();
      if (ex != 0) {
        ex += 1;
      }
      if (ex + pair.length() > TRACE_STATE_SIZE_LIMIT) {
        break;
      }
      if (sb.length() > 0) {
        sb.append(';');
      }
      sb.append(pair);
    }
    return sb.toString();
  }

  private static boolean isValueByte(char c) {
    return isLowerCaseAlphaNum(c) || isUpperCaseAlpha(c) || c == '.' || c == '_' || c == '-';
  }

  private static boolean isLowerCaseAlphaNum(char c) {
    return isLowerCaseAlpha(c) || isDigit(c);
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isLowerCaseAlpha(char c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isUpperCaseAlpha(char c) {
    return c >= 'A' && c <= 'Z';
  }

  private static final char[] HEX_DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  @CanIgnoreReturnValue
  static StringBuilder appendRandomValue(StringBuilder sb, long l) {
    for (int i = 52; i >= 0; i -= 4) {
      sb.append(HEX_DIGITS[(int) ((l >>> i) & 0xf)]);
    }
    return sb;
  }

  @CanIgnoreReturnValue
  static StringBuilder appendThreshold(StringBuilder sb, long l) {
    int numTrailingBits = Long.numberOfTrailingZeros(l | 0x80000000000000L);
    for (int i = 52; i >= numTrailingBits - 3; i -= 4) {
      sb.append(HEX_DIGITS[(int) ((l >>> i) & 0xf)]);
    }
    return sb;
  }

  private static long parseRandomValue(String s, int startIncl, int endIncl) {
    int len = endIncl - startIncl;
    if (len != 14) {
      return getInvalidRandomValue();
    }
    return parseHex(s, startIncl, len, getInvalidRandomValue());
  }

  private static long parseThreshold(String s, int startIncl, int endIncl) {
    int len = endIncl - startIncl;
    if (len > 14) {
      return getInvalidThreshold();
    }
    return parseHex(s, startIncl, len, getInvalidThreshold());
  }

  static long parseHex(String s, int startIncl, int len, long invalidReturnValue) {
    long r = 0;
    for (int i = 0; i < len; ++i) {
      long c = s.charAt(startIncl + i);
      long x;
      if (c >= '0' && c <= '9') {
        x = c - '0';
      } else if (c >= 'a' && c <= 'f') {
        x = c - 'a' + 10;
      } else {
        return invalidReturnValue;
      }
      r |= x << (52 - (i << 2));
    }
    return r;
  }

  /**
   * Parses the trace state from a given string.
   *
   * <p>If the string cannot be successfully parsed, a new empty {@code OtelTraceState2} is
   * returned.
   *
   * @param ts the string
   * @return the parsed trace state or an empty trace state in case of parsing errors
   */
  public static OtelTraceState parse(@Nullable String ts) {
    List<String> otherKeyValuePairs = null;
    long threshold = getInvalidThreshold();
    long randomValue = getInvalidRandomValue();

    if (ts == null || ts.isEmpty()) {
      return new OtelTraceState();
    }

    if (ts.length() > TRACE_STATE_SIZE_LIMIT) {
      return new OtelTraceState();
    }

    int startPos = 0;
    int len = ts.length();

    while (true) {
      int colonPos = startPos;
      for (; colonPos < len; colonPos++) {
        char c = ts.charAt(colonPos);
        if (!isLowerCaseAlpha(c) && (!isDigit(c) || colonPos == startPos)) {
          break;
        }
      }
      if (colonPos == startPos || colonPos == len || ts.charAt(colonPos) != ':') {
        return new OtelTraceState();
      }

      int separatorPos = colonPos + 1;
      while (separatorPos < len && isValueByte(ts.charAt(separatorPos))) {
        separatorPos++;
      }

      if (colonPos - startPos == SUBKEY_THRESHOLD.length()
          && ts.startsWith(SUBKEY_THRESHOLD, startPos)) {
        threshold = parseThreshold(ts, colonPos + 1, separatorPos);
      } else if (colonPos - startPos == SUBKEY_RANDOM_VALUE.length()
          && ts.startsWith(SUBKEY_RANDOM_VALUE, startPos)) {
        randomValue = parseRandomValue(ts, colonPos + 1, separatorPos);
      } else {
        if (otherKeyValuePairs == null) {
          otherKeyValuePairs = new ArrayList<>();
        }
        otherKeyValuePairs.add(ts.substring(startPos, separatorPos));
      }

      if (separatorPos < len && ts.charAt(separatorPos) != ';') {
        return new OtelTraceState();
      }

      if (separatorPos == len) {
        break;
      }

      startPos = separatorPos + 1;

      // test for a trailing ;
      if (startPos == len) {
        return new OtelTraceState();
      }
    }

    return new OtelTraceState(
        randomValue,
        threshold,
        (otherKeyValuePairs != null) ? otherKeyValuePairs : Collections.emptyList());
  }
}
