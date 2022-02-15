/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.util;

import java.util.concurrent.ThreadLocalRandom;

public class DefaultRandomGenerator implements RandomGenerator {

  private static final class ThreadLocalData {
    private long randomBits = 0;
    private int bitCount = 0;

    boolean nextRandomBit() {
      if ((bitCount & 0x3F) == 0) {
        randomBits = ThreadLocalRandom.current().nextLong();
      }
      boolean randomBit = ((randomBits >>> bitCount) & 1L) != 0L;
      bitCount += 1;
      return randomBit;
    }
  }

  private static final ThreadLocal<ThreadLocalData> THREAD_LOCAL_DATA =
      ThreadLocal.withInitial(ThreadLocalData::new);

  private static final DefaultRandomGenerator INSTANCE = new DefaultRandomGenerator();

  private DefaultRandomGenerator() {}

  public static RandomGenerator get() {
    return INSTANCE;
  }

  @Override
  public long nextLong() {
    return ThreadLocalRandom.current().nextLong();
  }

  @Override
  public boolean nextBoolean(double probability) {
    return RandomUtil.generateRandomBoolean(
        () -> THREAD_LOCAL_DATA.get().nextRandomBit(), probability);
  }

  @Override
  public int numberOfLeadingZerosOfRandomLong() {
    return RandomUtil.numberOfLeadingZerosOfRandomLong(
        () -> THREAD_LOCAL_DATA.get().nextRandomBit());
  }
}
