/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.util;

import java.util.BitSet;
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
   * Stochastically rounds the given floating-point value.
   *
   * <p>see https://en.wikipedia.org/wiki/Rounding#Stochastic_rounding
   *
   * @param randomGenerator a random generator
   * @param x the value to be rounded
   * @return the rounded value
   */
  public static long roundStochastically(RandomGenerator randomGenerator, double x) {
    long i = (long) Math.floor(x);
    if (randomGenerator.nextBoolean(x - i)) {
      return i + 1;
    } else {
      return i;
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

  /**
   * Generates a random bit set where a given number of 1-bits are randomly set.
   *
   * @param randomGenerator the random generator
   * @param numBits the total number of bits
   * @param numOneBits the number of 1-bits
   * @return a random bit set
   * @throws IllegalArgumentException if {@code 0 <= numOneBits <= numBits} is violated
   */
  public static BitSet generateRandomBitSet(
      RandomGenerator randomGenerator, int numBits, int numOneBits) {

    if (numOneBits < 0 || numOneBits > numBits) {
      throw new IllegalArgumentException();
    }

    BitSet result = new BitSet(numBits);
    int numZeroBits = numBits - numOneBits;

    // based on Fisher-Yates shuffling
    for (int i = Math.max(numZeroBits, numOneBits); i < numBits; ++i) {
      int j = randomGenerator.nextInt(i + 1);
      if (result.get(j)) {
        result.set(i);
      } else {
        result.set(j);
      }
    }
    if (numZeroBits < numOneBits) {
      result.flip(0, numBits);
    }

    return result;
  }
}
