/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.util;

import java.util.function.BooleanSupplier;

public final class RandomUtil {

  private RandomUtil() {}

  /**
   * Returns a pseudorandomly chosen {@code boolean} value where the probability of returning {@code
   * true} is predefined.
   *
   * @param randomBooleanSupplier a random boolean supplier
   * @param probability the probability of returning {@code true}
   * @return a random {@code boolean}
   */
  public static boolean generateRandomBoolean(
      BooleanSupplier randomBooleanSupplier, double probability) {
    while (true) {
      if (probability <= 0) {
        return false;
      }
      if (probability >= 1) {
        return true;
      }
      boolean b = probability > 0.5;
      if (randomBooleanSupplier.getAsBoolean()) {
        return b;
      }
      probability += probability;
      if (b) {
        probability -= 1;
      }
    }
  }

  /**
   * Returns the number of leading zeros of a uniform random 64-bit integer.
   *
   * @param randomBooleanSupplier a random boolean supplier
   * @return the truncated geometrically distributed random value
   */
  public static int numberOfLeadingZerosOfRandomLong(BooleanSupplier randomBooleanSupplier) {
    int count = 0;
    while (count < Long.SIZE && randomBooleanSupplier.getAsBoolean()) {
      count += 1;
    }
    return count;
  }
}
