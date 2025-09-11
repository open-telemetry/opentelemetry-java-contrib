/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class OtelTraceStateTest {

  private static String getXString(int len) {
    return Stream.generate(() -> "X").limit(len).collect(Collectors.joining());
  }

  @Test
  void test() {

    assertEquals("", OtelTraceState.parse("").serialize());
    assertEquals("", OtelTraceState.parse("").serialize());

    assertEquals("", OtelTraceState.parse("a").serialize());
    assertEquals("", OtelTraceState.parse("#").serialize());
    assertEquals("", OtelTraceState.parse(" ").serialize());

    assertEquals("rv:1234567890abcd", OtelTraceState.parse("rv:1234567890abcd").serialize());
    assertEquals("rv:01020304050607", OtelTraceState.parse("rv:01020304050607").serialize());
    assertEquals("", OtelTraceState.parse("rv:1234567890abcde").serialize());

    assertEquals("th:1234567890abcd", OtelTraceState.parse("th:1234567890abcd").serialize());
    assertEquals("th:01020304050607", OtelTraceState.parse("th:01020304050607").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:10000000000000").serialize());
    assertEquals("th:12345", OtelTraceState.parse("th:1234500000000").serialize());
    assertEquals("th:0", OtelTraceState.parse("th:0").serialize()); // TODO
    assertEquals("", OtelTraceState.parse("th:100000000000000").serialize());
    assertEquals("", OtelTraceState.parse("th:1234567890abcde").serialize());

    assertEquals(
        "th:1234567890abcd;rv:1234567890abcd;a:" + getXString(214) + ";x:3",
        OtelTraceState.parse("a:" + getXString(214) + ";rv:1234567890abcd;th:1234567890abcd;x:3")
            .serialize());
    assertEquals(
        "",
        OtelTraceState.parse("a:" + getXString(215) + ";rv:1234567890abcd;th:1234567890abcd;x:3")
            .serialize());

    assertEquals("", OtelTraceState.parse("th:x").serialize());
    assertEquals("", OtelTraceState.parse("th:100000000000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:10000000000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:1000000000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:100000000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:10000000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:1000000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:100000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:10000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:1000000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:100000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:10000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:1000").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:100").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:10").serialize());
    assertEquals("th:1", OtelTraceState.parse("th:1").serialize());

    assertEquals("th:10000000000001", OtelTraceState.parse("th:10000000000001").serialize());
    assertEquals("th:1000000000001", OtelTraceState.parse("th:10000000000010").serialize());
    assertEquals("", OtelTraceState.parse("rv:x").serialize());
    assertEquals("", OtelTraceState.parse("rv:100000000000000").serialize());
    assertEquals("rv:10000000000000", OtelTraceState.parse("rv:10000000000000").serialize());
    assertEquals("", OtelTraceState.parse("rv:1000000000000").serialize());
  }
}
