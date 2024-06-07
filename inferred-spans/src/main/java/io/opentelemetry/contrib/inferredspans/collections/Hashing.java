/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.collections;

/** Hashing functions for applying to integers. */
public class Hashing {
  /** Default load factor to be used in open addressing hashed data structures. */
  public static final float DEFAULT_LOAD_FACTOR = 0.55f;

  /**
   * Generate a hash for an int value. This is a no op.
   *
   * @param value to be hashed.
   * @return the hashed value.
   */
  public static int hash(final int value) {
    return value * 31;
  }

  /**
   * Generate a hash for an long value.
   *
   * @param value to be hashed.
   * @return the hashed value.
   */
  public static int hash(final long value) {
    long hash = value * 31;
    hash = (int) hash ^ (int) (hash >>> 32);

    return (int) hash;
  }

  /**
   * Generate a hash for a int value.
   *
   * @param value to be hashed.
   * @param mask mask to be applied that must be a power of 2 - 1.
   * @return the hash of the value.
   */
  public static int hash(final int value, final int mask) {
    final int hash = value * 31;

    return hash & mask;
  }

  /**
   * Generate a hash for a K value.
   *
   * @param <K> is the type of value
   * @param value to be hashed.
   * @param mask mask to be applied that must be a power of 2 - 1.
   * @return the hash of the value.
   */
  public static <K> int hash(final K value, final int mask) {
    final int hash = value.hashCode();

    return hash & mask;
  }

  /**
   * Generate a hash for a long value.
   *
   * @param value to be hashed.
   * @param mask mask to be applied that must be a power of 2 - 1.
   * @return the hash of the value.
   */
  public static int hash(final long value, final int mask) {
    long hash = value * 31;
    hash = (int) hash ^ (int) (hash >>> 32);

    return (int) hash & mask;
  }

  /**
   * Generate an even hash for a int value.
   *
   * @param value to be hashed.
   * @param mask mask to be applied that must be a power of 2 - 1.
   * @return the hash of the value which is always even.
   */
  public static int evenHash(final int value, final int mask) {
    final int hash = (value << 1) - (value << 8);

    return hash & mask;
  }

  /**
   * Generate an even hash for a long value.
   *
   * @param value to be hashed.
   * @param mask mask to be applied that must be a power of 2 - 1.
   * @return the hash of the value which is always even.
   */
  public static int evenHash(final long value, final int mask) {
    int hash = (int) value ^ (int) (value >>> 32);
    hash = (hash << 1) - (hash << 8);

    return hash & mask;
  }

  /**
   * Combined two 32 bit keys into a 64-bit compound.
   *
   * @param keyPartA to make the upper bits
   * @param keyPartB to make the lower bits.
   * @return the compound key
   */
  public static long compoundKey(final int keyPartA, final int keyPartB) {
    return ((long) keyPartA << 32) | (keyPartB & 0xFFFF_FFFFL);
  }

  public static int hashCode(long value) {
    return (int) (value ^ (value >>> 32));
  }
}
