/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateAdjustedCount;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidRandomValue;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.isValidRandomValue;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.isValidThreshold;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

public class ConsistentSamplingUtilTest {

  @Test
  void testCalculateSamplingProbability() {
    assertThat(calculateSamplingProbability(0L)).isEqualTo(0.);
    assertThat(calculateSamplingProbability(0x40000000000000L)).isEqualTo(0.25);
    assertThat(calculateSamplingProbability(0x80000000000000L)).isEqualTo(0.5);
    assertThat(calculateSamplingProbability(0x100000000000000L)).isEqualTo(1.);
    assertThatIllegalArgumentException().isThrownBy(() -> calculateSamplingProbability(-1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> calculateSamplingProbability(0x100000000000001L));
  }

  @Test
  void testCalculateThreshold() {
    assertThat(calculateThreshold(0.)).isEqualTo(0L);
    assertThat(calculateThreshold(0.25)).isEqualTo(0x40000000000000L);
    assertThat(calculateThreshold(0.5)).isEqualTo(0x80000000000000L);
    assertThat(calculateThreshold(1.)).isEqualTo(0x100000000000000L);
    assertThatIllegalArgumentException().isThrownBy(() -> calculateThreshold(Math.nextDown(0.)));
    assertThatIllegalArgumentException().isThrownBy(() -> calculateThreshold(Math.nextUp(1.)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> calculateThreshold(Double.POSITIVE_INFINITY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> calculateThreshold(Double.NEGATIVE_INFINITY));
    assertThatIllegalArgumentException().isThrownBy(() -> calculateThreshold(Double.NaN));
  }

  @Test
  void testGetInvalidRandomValue() {
    assertThat(isValidRandomValue(getInvalidRandomValue())).isFalse();
  }

  @Test
  void testGetInvalidThreshold() {
    assertThat(isValidThreshold(getInvalidThreshold())).isFalse();
  }

  @Test
  void testGetMaxThreshold() {
    assertThat(ConsistentSamplingUtil.getMaxThreshold()).isEqualTo(0x100000000000000L);
  }

  @Test
  void testGetMaxRandomValue() {
    assertThat(ConsistentSamplingUtil.getMaxRandomValue()).isEqualTo(0xFFFFFFFFFFFFFFL);
  }

  @Test
  void testCalculateAdjustedCount() {
    assertThat(calculateAdjustedCount(0L)).isZero();
    assertThat(calculateAdjustedCount(0x40000000000000L)).isEqualTo(4.);
    assertThat(calculateAdjustedCount(0x80000000000000L)).isEqualTo(2.);
    assertThat(calculateAdjustedCount(0x100000000000000L)).isOne();
    assertThat(calculateAdjustedCount(-1)).isOne();
    assertThat(calculateAdjustedCount(0x100000000000001L)).isOne();
  }

  @Test
  void testAppendLast56BitHexEncoded() {
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncoded(new StringBuilder(), 0x3a436f7842456L))
        .hasToString("03a436f7842456");
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncoded(
                new StringBuilder(), 0x3a436f7842456abL))
        .hasToString("a436f7842456ab");
    assertThat(ConsistentSamplingUtil.appendLast56BitHexEncoded(new StringBuilder(), 0L))
        .hasToString("00000000000000");
  }

  @Test
  void testAppendLast56BitHexEncodedWithoutTrailingZeros() {
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                new StringBuilder(), 0x3a436f7842456L))
        .hasToString("03a436f7842456");
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                new StringBuilder(), 0x3a436f7842456abL))
        .hasToString("a436f7842456ab");
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                new StringBuilder(), 0x80000000000000L))
        .hasToString("8");
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                new StringBuilder(), 0x11000000000000L))
        .hasToString("11");
    assertThat(
            ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                new StringBuilder(), 0L))
        .hasToString("0");
  }
}
