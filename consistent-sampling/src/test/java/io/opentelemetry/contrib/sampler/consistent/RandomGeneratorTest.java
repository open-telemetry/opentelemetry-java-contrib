/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.BitSet;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import org.hipparchus.stat.inference.GTest;
import org.junit.jupiter.api.Test;

class RandomGeneratorTest {

  private static void testGenerateRandomBitSet(long seed, int numBits, int numOneBits) {

    int numCycles = 100000;

    SplittableRandom splittableRandom = new SplittableRandom(seed);
    RandomGenerator randomGenerator = RandomGenerator.create(splittableRandom::nextLong);

    long[] observed = new long[numBits];
    double[] expected = DoubleStream.generate(() -> 1.).limit(numBits).toArray();

    for (int i = 0; i < numCycles; ++i) {
      BitSet bitSet = randomGenerator.generateRandomBitSet(numBits, numOneBits);
      bitSet.stream().forEach(k -> observed[k] += 1);
      assertThat(bitSet.cardinality()).isEqualTo(numOneBits);
    }
    if (numBits > 1) {
      assertThat(new GTest().gTest(expected, observed)).isGreaterThan(0.01);
    } else if (numBits == 1) {
      assertThat(observed[0]).isEqualTo(numOneBits * (long) numCycles);
    } else {
      fail("numBits was non-positive!");
    }
  }

  @Test
  void testGenerateRandomBitSet() {
    testGenerateRandomBitSet(0x4a5580b958d52182L, 1, 0);
    testGenerateRandomBitSet(0x529dff14b0ce7414L, 1, 1);
    testGenerateRandomBitSet(0x2d3f673a9e1da536L, 2, 0);
    testGenerateRandomBitSet(0xb9a6735e64361bacL, 2, 1);
    testGenerateRandomBitSet(0xb5aafedc7031506fL, 2, 2);
    testGenerateRandomBitSet(0xaecabe7698971ee1L, 3, 0);
    testGenerateRandomBitSet(0x119ccf35dc52b34dL, 3, 1);
    testGenerateRandomBitSet(0xcaf2b7a98f194ce2L, 3, 2);
    testGenerateRandomBitSet(0xe28e8cc3d3de0c2aL, 3, 3);
    testGenerateRandomBitSet(0xb69989dce9cc8b34L, 4, 0);
    testGenerateRandomBitSet(0x6575d4c848c95dc8L, 4, 1);
    testGenerateRandomBitSet(0xed0ad0525ad632e9L, 4, 2);
    testGenerateRandomBitSet(0x34db9303b405a706L, 4, 3);
    testGenerateRandomBitSet(0x8e97972893044140L, 4, 4);
    testGenerateRandomBitSet(0x47f966b8f28dac77L, 5, 0);
    testGenerateRandomBitSet(0x7996db4a5f1e4680L, 5, 1);
    testGenerateRandomBitSet(0x577fcf18bbc0ba30L, 5, 2);
    testGenerateRandomBitSet(0x36b1ed999d2986b0L, 5, 3);
    testGenerateRandomBitSet(0xa8e099ed958d03bbL, 5, 4);
    testGenerateRandomBitSet(0xc2b50bbf3263b414L, 5, 5);
    testGenerateRandomBitSet(0x2994550582b091e9L, 6, 0);
    testGenerateRandomBitSet(0xd2797c539136f6faL, 6, 1);
    testGenerateRandomBitSet(0xf3ffae1d93983fd9L, 6, 2);
    testGenerateRandomBitSet(0x281e0f9873455ea6L, 6, 3);
    testGenerateRandomBitSet(0x5344c2887e30d621L, 6, 4);
    testGenerateRandomBitSet(0xa8f4ed6e3e1cf385L, 6, 5);
    testGenerateRandomBitSet(0x6bd0f9f11520ae57L, 6, 6);

    testGenerateRandomBitSet(0x514f52732c193e62L, 1000, 1);
    testGenerateRandomBitSet(0xe214063ae29d9802L, 1000, 10);
    testGenerateRandomBitSet(0x602fdb45063e7b0fL, 1000, 990);
    testGenerateRandomBitSet(0xe0ef0cb214de3ec0L, 1000, 999);
  }
}
