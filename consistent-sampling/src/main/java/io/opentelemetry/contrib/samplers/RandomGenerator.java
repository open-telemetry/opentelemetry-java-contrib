/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

final class RandomGenerator {

  private final LongSupplier threadSafeRandomLongSupplier;

  private static final class ThreadLocalData {
    private long randomBits = 0;
    private int bitCount = 0;

    private boolean nextRandomBit(LongSupplier threadSafeRandomLongSupplier) {
      if ((bitCount & 0x3F) == 0) {
        randomBits = threadSafeRandomLongSupplier.getAsLong();
      }
      boolean randomBit = ((randomBits >>> bitCount) & 1L) != 0L;
      bitCount += 1;
      return randomBit;
    }

    /**
     * Returns a pseudorandomly chosen {@code boolean} value where the probability of returning
     * {@code true} is predefined.
     *
     * <p>{@code true} needs to be returned with a success probability of {@code probability}. If
     * the success probability is greater than 50% ({@code probability > 0.5}), the same can be
     * achieved by returning {@code true} with a probability of 50%, and returning the result of a
     * Bernoulli trial with a probability of {@code 2 * probability - 1}. The resulting success
     * probability will be the same as {@code 0.5 + 0.5 * (2 * probability - 1) = probability}.
     * Similarly, if the success probability is smaller than 50% ({@code probability <= 0.5}),
     * {@code false} is returned with a probability of 50%. Otherwise, the result of a Bernoulli
     * trial with success probability of {@code 2 * probability} is returned. Again, the resulting
     * success probability is exactly as desired because {@code 0.5 * (2 * probability) =
     * probability}. Recursive continuation of this approach allows realizing Bernoulli trials with
     * arbitrary success probabilities using just few random bits.
     *
     * @param threadSafeRandomLongSupplier a thread-safe random long supplier
     * @param probability the probability of returning {@code true}
     * @return a random {@code boolean}
     */
    private boolean generateRandomBoolean(
        LongSupplier threadSafeRandomLongSupplier, double probability) {
      while (true) {
        if (probability <= 0) {
          return false;
        }
        if (probability >= 1) {
          return true;
        }
        boolean b = probability > 0.5;
        if (nextRandomBit(threadSafeRandomLongSupplier)) {
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
     * @param threadSafeRandomLongSupplier a thread-safe random long supplier
     * @return the number of leading zeros
     */
    private int numberOfLeadingZerosOfRandomLong(LongSupplier threadSafeRandomLongSupplier) {
      int count = 0;
      while (count < Long.SIZE && nextRandomBit(threadSafeRandomLongSupplier)) {
        count += 1;
      }
      return count;
    }
  }

  private static final ThreadLocal<ThreadLocalData> THREAD_LOCAL_DATA =
      ThreadLocal.withInitial(ThreadLocalData::new);

  private static final RandomGenerator INSTANCE =
      new RandomGenerator(() -> ThreadLocalRandom.current().nextLong());

  private RandomGenerator(LongSupplier threadSafeRandomLongSupplier) {
    this.threadSafeRandomLongSupplier = requireNonNull(threadSafeRandomLongSupplier);
  }

  /**
   * Creates a new random generator using the given thread-safe random long supplier as random
   * source.
   *
   * @param threadSafeRandomLongSupplier a thread-safe random long supplier
   * @return a random generator
   */
  public static RandomGenerator create(LongSupplier threadSafeRandomLongSupplier) {
    return new RandomGenerator(threadSafeRandomLongSupplier);
  }

  /**
   * Returns a default random generator.
   *
   * @return a random generator
   */
  public static RandomGenerator getDefault() {
    return INSTANCE;
  }

  /**
   * Returns a pseudorandomly chosen {@code boolean} value where the probability of returning {@code
   * true} is predefined.
   *
   * @param probability the probability of returning {@code true}
   * @return a random {@code boolean}
   */
  public boolean nextBoolean(double probability) {
    return THREAD_LOCAL_DATA.get().generateRandomBoolean(threadSafeRandomLongSupplier, probability);
  }

  /**
   * Returns the number of leading zeros of a uniform random 64-bit integer.
   *
   * @return the number of leading zeros
   */
  public int numberOfLeadingZerosOfRandomLong() {
    return THREAD_LOCAL_DATA.get().numberOfLeadingZerosOfRandomLong(threadSafeRandomLongSupplier);
  }
}
