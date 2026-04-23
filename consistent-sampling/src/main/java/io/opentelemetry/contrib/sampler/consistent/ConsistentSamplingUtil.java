/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

final class ConsistentSamplingUtil {

  private static final int RANDOM_VALUE_BITS = 56;
  // corresponds to 0% sampling probability
  static final long MAX_THRESHOLD = 1L << RANDOM_VALUE_BITS;
  // the special "invalid threshold" sentinel
  static final long INVALID_THRESHOLD = -1;

  private ConsistentSamplingUtil() {}

  /**
   * Returns the sampling probability corresponding to the given rejection threshold.
   *
   * <p>The returned value does not always exactly match the applied sampling probability, since
   * some least significant binary digits may not be representable by double-precision floating
   * point numbers.
   */
  static double calculateSamplingProbability(long threshold) {
    checkThreshold(threshold);
    return (MAX_THRESHOLD - threshold) * 0x1p-56;
  }

  /**
   * Returns the closest rejection threshold that can be used to realize sampling with the given
   * probability.
   */
  static long calculateThreshold(double samplingProbability) {
    checkProbability(samplingProbability);
    return MAX_THRESHOLD - Math.round(samplingProbability * 0x1p56);
  }

  static boolean isValidThreshold(long threshold) {
    return 0 <= threshold && threshold <= MAX_THRESHOLD;
  }

  private static void checkThreshold(long threshold) {
    if (!isValidThreshold(threshold)) {
      throw new IllegalArgumentException("The threshold must be in the range [0,2^56]!");
    }
  }

  private static void checkProbability(double probability) {
    if (!(probability >= 0) || !(probability <= 1)) {
      throw new IllegalArgumentException("The probability must be in the range [0,1]!");
    }
  }

  private static final char[] HEX_DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  static String encodeLast56BitHexWithoutTrailingZeros(long l) {
    int numTrailingBits = Long.numberOfTrailingZeros(l | 0x80000000000000L);
    StringBuilder sb = new StringBuilder();
    for (int i = 52; i >= numTrailingBits - 3; i -= 4) {
      sb.append(HEX_DIGITS[(int) ((l >>> i) & 0xf)]);
    }
    return sb.toString();
  }
}
