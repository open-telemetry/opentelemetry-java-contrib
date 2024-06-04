/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.HEX_DIGITS;

import java.util.SplittableRandom;

public final class TestUtil {

  private TestUtil() {}

  static String generateRandomTraceId(SplittableRandom random) {
    StringBuilder sb = new StringBuilder(16);
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
