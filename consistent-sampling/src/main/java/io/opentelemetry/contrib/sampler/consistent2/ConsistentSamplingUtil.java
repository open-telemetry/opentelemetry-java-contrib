/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent2;

public final class ConsistentSamplingUtil {

  private static final int RANDOM_VALUE_BITS = 56;
  private static final long MAX_THRESHOLD = 1L << RANDOM_VALUE_BITS;
  private static final long MAX_RANDOM_VALUE = MAX_THRESHOLD - 1;
  private static final long INVALID_THRESHOLD = -1;
  private static final long INVALID_RANDOM_VALUE = -1;

  private ConsistentSamplingUtil() {}

  /**
   * Returns for a given threshold the corresponding sampling probability.
   *
   * <p>The returned value does not always exactly match the applied sampling probability, since
   * some least significant binary digits may not be represented by double-precision floating point
   * numbers.
   *
   * @param threshold the threshold
   * @return the sampling probability
   */
  public static double calculateSamplingProbability(long threshold) {
    checkThreshold(threshold);
    return threshold * 0x1p-56;
  }

  /**
   * Returns the closest sampling threshold that can be used to realize sampling with the given
   * probability.
   *
   * @param samplingProbability the sampling probability
   * @return the threshold
   */
  public static long calculateThreshold(double samplingProbability) {
    checkProbability(samplingProbability);
    return Math.round(samplingProbability * 0x1p56);
  }

  /**
   * Calculates the adjusted count from a given threshold.
   *
   * <p>Returns 1, if the threshold is invalid.
   *
   * <p>Returns {@link Double#POSITIVE_INFINITY}, if the threshold is 0.
   *
   * @param threshold the threshold
   * @return the adjusted count
   */
  public static double calculateAdjustedCount(long threshold) {
    if (isValidThreshold(threshold)) {
      return 0x1p56 / threshold;
    } else {
      return 1.;
    }
  }

  /**
   * Returns an invalid random value.
   *
   * <p>{@code isValidRandomValue(getInvalidRandomValue())} will always return true.
   *
   * @return an invalid random value
   */
  public static long getInvalidRandomValue() {
    return INVALID_RANDOM_VALUE;
  }

  /**
   * Returns an invalid threshold.
   *
   * <p>{@code isValidThreshold(getInvalidThreshold())} will always return true.
   *
   * @return an invalid threshold value
   */
  public static long getInvalidThreshold() {
    return INVALID_THRESHOLD;
  }

  public static long getMaxRandomValue() {
    return MAX_RANDOM_VALUE;
  }

  public static long getMaxThreshold() {
    return MAX_THRESHOLD;
  }

  public static boolean isValidRandomValue(long randomValue) {
    return 0 <= randomValue && randomValue <= getMaxRandomValue();
  }

  public static boolean isValidThreshold(long threshold) {
    return 0 <= threshold && threshold <= getMaxThreshold();
  }

  public static boolean isValidProbability(double probability) {
    return 0 <= probability && probability <= 1;
  }

  static void checkThreshold(long threshold) {
    if (!isValidThreshold(threshold)) {
      throw new IllegalArgumentException("The threshold must be in the range [0,2^56]!");
    }
  }

  static void checkProbability(double probability) {
    if (!isValidProbability(probability)) {
      throw new IllegalArgumentException("The probability must be in the range [0,1]!");
    }
  }
}
