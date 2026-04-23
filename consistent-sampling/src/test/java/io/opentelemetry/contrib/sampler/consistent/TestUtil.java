/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import java.util.SplittableRandom;

final class TestUtil {

  private static final char[] HEX_DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  private TestUtil() {}

  static String generateRandomTraceId(SplittableRandom random) {
    StringBuilder sb = new StringBuilder(32);
    long hi = random.nextLong();
    long lo = random.nextLong();
    for (int i = 0; i < 64; i += 4) {
      sb.append(HEX_DIGITS[(int) (hi >>> i) & 0xF]);
    }
    for (int i = 0; i < 64; i += 4) {
      sb.append(HEX_DIGITS[(int) (lo >>> i) & 0xF]);
    }
    return sb.toString();
  }
}
