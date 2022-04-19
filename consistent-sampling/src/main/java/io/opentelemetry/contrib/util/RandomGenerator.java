/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.util;

/** A random generator. */
public interface RandomGenerator {

  /**
   * Returns a pseudorandomly chosen {@code long} value.
   *
   * @return a pseudorandomly chosen {@code long} value
   */
  long nextLong();

  /**
   * Returns a pseudorandomly chosen {@code boolean} value where the probability of returning {@code
   * true} is predefined.
   *
   * @param probability the probability of returning {@code true}
   * @return a random {@code boolean}
   */
  default boolean nextBoolean(double probability) {
    long randomBits = 0;
    int bitCounter = 0;
    while (true) {
      if (probability <= 0) {
        return false;
      }
      if (probability >= 1) {
        return true;
      }
      boolean b = probability > 0.5;
      if ((bitCounter & 0x3f) == 0) {
        randomBits = nextLong();
      }
      if (((randomBits >>> bitCounter) & 1L) == 1L) {
        return b;
      }
      bitCounter += 1;
      probability += probability;
      if (b) {
        probability -= 1;
      }
    }
  }

  /**
   * Returns the number of leading zeros of a uniform random 64-bit integer.
   *
   * @return the number of leading zeros
   */
  default int numberOfLeadingZerosOfRandomLong() {
    return Long.numberOfLeadingZeros(nextLong());
  }
}
