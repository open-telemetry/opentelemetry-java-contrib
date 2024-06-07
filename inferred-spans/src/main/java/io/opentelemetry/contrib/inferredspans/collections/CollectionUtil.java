/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.collections;

/** Utility functions for collection objects. */
@SuppressWarnings({"PrivateConstructorForUtilityClass", "UnnecessaryFinal"})
public class CollectionUtil {
  /**
   * Validate that a load factor is in the range of 0.1 to 0.9.
   *
   * <p>Load factors in the range 0.5 - 0.7 are recommended for open-addressing with linear probing.
   *
   * @param loadFactor to be validated.
   */
  public static void validateLoadFactor(final float loadFactor) {
    if (loadFactor < 0.1f || loadFactor > 0.9f) {
      throw new IllegalArgumentException(
          "load factor must be in the range of 0.1 to 0.9: " + loadFactor);
    }
  }

  /**
   * Fast method of finding the next power of 2 greater than or equal to the supplied value.
   *
   * <p>If the value is &lt;= 0 then 1 will be returned.
   *
   * <p>This method is not suitable for {@link Integer#MIN_VALUE} or numbers greater than 2^30. When
   * provided then {@link Integer#MIN_VALUE} will be returned.
   *
   * @param value from which to search for next power of 2.
   * @return The next power of 2 or the value itself if it is a power of 2.
   */
  public static int findNextPositivePowerOfTwo(final int value) {
    return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
  }

  /**
   * Fast method of finding the next power of 2 greater than or equal to the supplied value.
   *
   * <p>If the value is &lt;= 0 then 1 will be returned.
   *
   * <p>This method is not suitable for {@link Long#MIN_VALUE} or numbers greater than 2^62. When
   * provided then {@link Long#MIN_VALUE} will be returned.
   *
   * @param value from which to search for next power of 2.
   * @return The next power of 2 or the value itself if it is a power of 2.
   */
  public static long findNextPositivePowerOfTwo(final long value) {
    return 1L << (Long.SIZE - Long.numberOfLeadingZeros(value - 1));
  }
}
