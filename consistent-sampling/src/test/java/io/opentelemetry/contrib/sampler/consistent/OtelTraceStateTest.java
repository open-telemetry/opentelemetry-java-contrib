/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

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

    assertThat(OtelTraceState.parse("p:5").serialize()).isEqualTo("p:5");
    assertThat(OtelTraceState.parse("p:63").serialize()).isEqualTo("p:63");
    assertThat(OtelTraceState.parse("p:64").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("p:5;").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("p:99").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("p:").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("p:232").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("x;p:5").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("p:5;x").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("x:3;p:5").serialize()).isEqualTo("p:5;x:3");
    assertThat(OtelTraceState.parse("p:5;x:3").serialize()).isEqualTo("p:5;x:3");
    assertThat(OtelTraceState.parse("p:5;x:3;").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("a:" + getXString(246) + ";p:5;x:3").serialize())
        .isEqualTo("p:5;a:" + getXString(246) + ";x:3");
    assertThat(OtelTraceState.parse("a:" + getXString(247) + ";p:5;x:3").serialize()).isEqualTo("");

    assertThat(OtelTraceState.parse("r:5").serialize()).isEqualTo("r:5");
    assertThat(OtelTraceState.parse("r:62").serialize()).isEqualTo("r:62");
    assertThat(OtelTraceState.parse("r:63").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("r:5;").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("r:99").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("r:").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("r:232").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("x;r:5").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("r:5;x").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("x:3;r:5").serialize()).isEqualTo("r:5;x:3");
    assertThat(OtelTraceState.parse("r:5;x:3").serialize()).isEqualTo("r:5;x:3");
    assertThat(OtelTraceState.parse("r:5;x:3;").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("a:" + getXString(246) + ";r:5;x:3").serialize())
        .isEqualTo("r:5;a:" + getXString(246) + ";x:3");
    assertThat(OtelTraceState.parse("a:" + getXString(247) + ";r:5;x:3").serialize()).isEqualTo("");

    assertThat(OtelTraceState.parse("r:5;p:7").serialize()).isEqualTo("p:7;r:5");
    assertThat(OtelTraceState.parse("r:5;p:4").serialize()).isEqualTo("p:4;r:5");
    assertThat(OtelTraceState.parse("r:5;p:7").serialize()).isEqualTo("p:7;r:5");
    assertThat(OtelTraceState.parse("r:5;p:4").serialize()).isEqualTo("p:4;r:5");

    assertThat(OtelTraceState.parse("r:5;r:6").serialize()).isEqualTo("r:6");
    assertThat(OtelTraceState.parse("p:5;p:6;r:10").serialize()).isEqualTo("p:6;r:10");
    assertThat(OtelTraceState.parse("p5;p:6;r:10").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("p5:3;p:6;r:10").serialize()).isEqualTo("p:6;r:10;p5:3");
    assertThat(OtelTraceState.parse(":p:6;r:10").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse(";p:6;r:10").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("_;p:6;r:10").serialize()).isEqualTo("");
    assertThat(OtelTraceState.parse("5;p:6;r:10").serialize()).isEqualTo("");
  }
}
