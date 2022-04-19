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
   * <p>{@code true} needs to be returned with a success probability of {@code probability}. If the
   * success probability is greater than 50% ({@code probability > 0.5}), the same can be achieved
   * by returning {@code true} with a probability of 50%, and returning the result of a Bernoulli
   * trial with a probability of {@code 2 * probability - 1}. The resulting success probability will
   * be the same as {@code 0.5 + 0.5 * (2 * probability - 1) = probability}. Similarly, if the
   * success probability is smaller than 50% ({@code probability <= 0.5}), {@code false} is returned
   * with a probability of 50%. Otherwise, the result of a Bernoulli trial with success probability
   * of {@code 2 * probability} is returned. Again, the resulting success probability is exactly as
   * desired because {@code 0.5 * (2 * probability) = probability}. Recursive continuation of this
   * approach allows realizing Bernoulli trials with arbitrary success probabilities using just few
   * random bits.
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
