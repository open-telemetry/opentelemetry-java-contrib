/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static java.util.Objects.requireNonNull;

import java.util.BitSet;
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

  private final ThreadLocal<ThreadLocalData> threadLocalData =
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
    return threadLocalData.get().generateRandomBoolean(threadSafeRandomLongSupplier, probability);
  }

  /**
   * Returns the number of leading zeros of a uniform random 64-bit integer.
   *
   * @return the number of leading zeros
   */
  public int numberOfLeadingZerosOfRandomLong() {
    return threadLocalData.get().numberOfLeadingZerosOfRandomLong(threadSafeRandomLongSupplier);
  }

  /**
   * Returns a pseudorandomly chosen {@code long} value.
   *
   * @return a pseudorandomly chosen {@code long} value
   */
  public long nextLong() {
    return threadSafeRandomLongSupplier.getAsLong();
  }

  /**
   * Stochastically rounds the given floating-point value.
   *
   * <p>see https://en.wikipedia.org/wiki/Rounding#Stochastic_rounding
   *
   * @param x the value to be rounded
   * @return the rounded value
   */
  public long roundStochastically(double x) {
    long i = (long) Math.floor(x);
    if (nextBoolean(x - i)) {
      return i + 1;
    } else {
      return i;
    }
  }

  /**
   * Returns a pseudorandomly chosen {@code int} value between zero (inclusive) and the specified
   * bound (exclusive).
   *
   * <p>The implementation is based on Daniel Lemire's algorithm as described in "Fast random
   * integer generation in an interval." ACM Transactions on Modeling and Computer Simulation
   * (TOMACS) 29.1 (2019): 3.
   *
   * @param bound the upper bound (exclusive) for the returned value. Must be positive.
   * @return a pseudorandomly chosen {@code int} value between zero (inclusive) and the bound
   *     (exclusive)
   * @throws IllegalArgumentException if {@code bound} is not positive
   */
  private int nextInt(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException();
    }
    long x = nextLong() >>> 33; // use only 31 random bits
    long m = x * bound;
    int l = (int) m & 0x7FFFFFFF;
    if (l < bound) {
      int t = (-bound & 0x7FFFFFFF) % bound;
      while (l < t) {
        x = nextLong() >>> 33; // use only 31 random bits
        m = x * bound;
        l = (int) m & 0x7FFFFFFF;
      }
    }
    return (int) (m >>> 31);
  }

  /**
   * Generates a random bit set where a given number of 1-bits are randomly set.
   *
   * @param numBits the total number of bits
   * @param numOneBits the number of 1-bits
   * @return a random bit set
   * @throws IllegalArgumentException if {@code 0 <= numOneBits <= numBits} is violated
   */
  public BitSet generateRandomBitSet(int numBits, int numOneBits) {

    if (numOneBits < 0 || numOneBits > numBits) {
      throw new IllegalArgumentException();
    }

    BitSet result = new BitSet(numBits);
    int numZeroBits = numBits - numOneBits;

    // based on Fisher-Yates shuffling
    for (int i = Math.max(numZeroBits, numOneBits); i < numBits; ++i) {
      int j = nextInt(i + 1);
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
