/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class OtelTraceStateTest {

  private static String getXString(int len) {
    return Stream.generate(() -> "X").limit(len).collect(Collectors.joining());
  }

  @Test
  void test() {

    assertThat(OtelTraceState.parse("").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("").serialize()).isEqualTo("");

    assertThat(OtelTraceState.parse("a").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("#").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse(" ").serialize()).isEqualTo("");

    assertThat(OtelTraceState.parse("rv:1234567890abcd").serialize())
        .isEqualTo("rv:1234567890abcd");
    assertThat(OtelTraceState.parse("rv:01020304050607").serialize())
        .isEqualTo("rv:01020304050607");
    assertThat(OtelTraceState.parse("rv:1234567890abcde").serialize()).isEqualTo("");

    assertThat(OtelTraceState.parse("th:1234567890abcd").serialize())
        .isEqualTo("th:1234567890abcd");
    assertThat(OtelTraceState.parse("th:01020304050607").serialize())
        .isEqualTo("th:01020304050607");
    assertThat(OtelTraceState.parse("th:10000000000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:1234500000000").serialize()).isEqualTo("th:12345");
    assertThat(OtelTraceState.parse("th:0").serialize()).isEqualTo("th:0"); // TODO
    assertThat(OtelTraceState.parse("th:100000000000000").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("th:1234567890abcde").serialize()).isEqualTo("");

    assertThat(
            OtelTraceState.parse(
                    "a:" + getXString(214) + ";rv:1234567890abcd;th:1234567890abcd;x:3")
                .serialize())
        .isEqualTo("th:1234567890abcd;rv:1234567890abcd;a:" + getXString(214) + ";x:3");
    assertThat(
            OtelTraceState.parse(
                    "a:" + getXString(215) + ";rv:1234567890abcd;th:1234567890abcd;x:3")
                .serialize())
        .isEqualTo("");

    assertThat(OtelTraceState.parse("th:x").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("th:100000000000000").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("th:10000000000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:1000000000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:100000000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:10000000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:1000000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:100000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:10000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:1000000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:100000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:10000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:1000").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:100").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:10").serialize()).isEqualTo("th:1");
    assertThat(OtelTraceState.parse("th:1").serialize()).isEqualTo("th:1");

    assertThat(OtelTraceState.parse("th:10000000000001").serialize())
        .isEqualTo("th:10000000000001");
    assertThat(OtelTraceState.parse("th:10000000000010").serialize()).isEqualTo("th:1000000000001");
    assertThat(OtelTraceState.parse("rv:x").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("rv:100000000000000").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("rv:10000000000000").serialize())
        .isEqualTo("rv:10000000000000");
    assertThat(OtelTraceState.parse("rv:1000000000000").serialize()).isEqualTo("");
  }
}
