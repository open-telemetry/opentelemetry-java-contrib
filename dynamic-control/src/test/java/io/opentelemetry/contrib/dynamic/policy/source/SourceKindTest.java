/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SourceKindTest {

  @Test
  void configValuesAreStableLowercase() {
    assertThat(SourceKind.FILE.configValue()).isEqualTo("file");
    assertThat(SourceKind.OPAMP.configValue()).isEqualTo("opamp");
    assertThat(SourceKind.HTTP.configValue()).isEqualTo("http");
    assertThat(SourceKind.CUSTOM.configValue()).isEqualTo("custom");
  }

  @Test
  void fromConfigValueParsesCaseInsensitive() {
    assertThat(SourceKind.fromConfigValue("FILE")).isEqualTo(SourceKind.FILE);
    assertThat(SourceKind.fromConfigValue("Opamp")).isEqualTo(SourceKind.OPAMP);
  }

  @Test
  void fromConfigValueTrimsWhitespace() {
    assertThat(SourceKind.fromConfigValue("  http  ")).isEqualTo(SourceKind.HTTP);
  }

  @Test
  void fromConfigValueRejectsNullAndUnknown() {
    assertThatThrownBy(() -> SourceKind.fromConfigValue(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> SourceKind.fromConfigValue("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown source kind");
  }
}
