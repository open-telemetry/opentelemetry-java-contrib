/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OtelTraceStateTest {

  private static String getXString(int len) {
    return Stream.generate(() -> "X").limit(len).collect(Collectors.joining());
  }

  @Test
  public void test() {

    Assertions.assertEquals("", OtelTraceState.parse("").serialize());
    assertEquals("", OtelTraceState.parse("").serialize());

    assertEquals("", OtelTraceState.parse("a").serialize());
    assertEquals("", OtelTraceState.parse("#").serialize());
    assertEquals("", OtelTraceState.parse(" ").serialize());

    assertEquals("p:5", OtelTraceState.parse("p:5").serialize());
    assertEquals("p:63", OtelTraceState.parse("p:63").serialize());
    assertEquals("", OtelTraceState.parse("p:64").serialize());
    assertEquals("", OtelTraceState.parse("p:5;").serialize());
    assertEquals("", OtelTraceState.parse("p:99").serialize());
    assertEquals("", OtelTraceState.parse("p:").serialize());
    assertEquals("", OtelTraceState.parse("p:232").serialize());
    assertEquals("", OtelTraceState.parse("x;p:5").serialize());
    assertEquals("", OtelTraceState.parse("p:5;x").serialize());
    assertEquals("p:5;x:3", OtelTraceState.parse("x:3;p:5").serialize());
    assertEquals("p:5;x:3", OtelTraceState.parse("p:5;x:3").serialize());
    assertEquals("", OtelTraceState.parse("p:5;x:3;").serialize());
    assertEquals(
        "p:5;a:" + getXString(246) + ";x:3",
        OtelTraceState.parse("a:" + getXString(246) + ";p:5;x:3").serialize());
    assertEquals("", OtelTraceState.parse("a:" + getXString(247) + ";p:5;x:3").serialize());

    assertEquals("r:5", OtelTraceState.parse("r:5").serialize());
    assertEquals("r:62", OtelTraceState.parse("r:62").serialize());
    assertEquals("", OtelTraceState.parse("r:63").serialize());
    assertEquals("", OtelTraceState.parse("r:5;").serialize());
    assertEquals("", OtelTraceState.parse("r:99").serialize());
    assertEquals("", OtelTraceState.parse("r:").serialize());
    assertEquals("", OtelTraceState.parse("r:232").serialize());
    assertEquals("", OtelTraceState.parse("x;r:5").serialize());
    assertEquals("", OtelTraceState.parse("r:5;x").serialize());
    assertEquals("r:5;x:3", OtelTraceState.parse("x:3;r:5").serialize());
    assertEquals("r:5;x:3", OtelTraceState.parse("r:5;x:3").serialize());
    assertEquals("", OtelTraceState.parse("r:5;x:3;").serialize());
    assertEquals(
        "r:5;a:" + getXString(246) + ";x:3",
        OtelTraceState.parse("a:" + getXString(246) + ";r:5;x:3").serialize());
    assertEquals("", OtelTraceState.parse("a:" + getXString(247) + ";r:5;x:3").serialize());

    assertEquals("p:7;r:5", OtelTraceState.parse("r:5;p:7").serialize());
    assertEquals("p:4;r:5", OtelTraceState.parse("r:5;p:4").serialize());
    assertEquals("p:7;r:5", OtelTraceState.parse("r:5;p:7").serialize());
    assertEquals("p:4;r:5", OtelTraceState.parse("r:5;p:4").serialize());

    assertEquals("r:6", OtelTraceState.parse("r:5;r:6").serialize());
    assertEquals("p:6;r:10", OtelTraceState.parse("p:5;p:6;r:10").serialize());
    assertEquals("", OtelTraceState.parse("p5;p:6;r:10").serialize());
  }
}
