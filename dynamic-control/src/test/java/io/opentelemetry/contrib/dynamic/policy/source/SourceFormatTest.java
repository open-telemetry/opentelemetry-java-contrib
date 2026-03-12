/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SourceFormatTest {

  @Test
  void configValuesAreStable() {
    assertThat(SourceFormat.JSON.configValue()).isEqualTo("json");
    assertThat(SourceFormat.KEYVALUE.configValue()).isEqualTo("keyvalue");
  }

  @Test
  void parseDelegatesToJsonParser() {
    List<SourceWrapper> parsed = SourceFormat.JSON.parse("{\"trace-sampling\": 0.5}");

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(JsonSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseDelegatesToKeyValueParser() {
    List<SourceWrapper> parsed = SourceFormat.KEYVALUE.parse("trace-sampling=0.5");

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(KeyValueSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseSupportsEmptyInputAcrossFormats() {
    assertThat(SourceFormat.JSON.parse("[]")).isEmpty();
    assertThat(SourceFormat.KEYVALUE.parse("")).isEmpty();
    assertThat(SourceFormat.KEYVALUE.parse("\n   \r\n")).isEmpty();
  }

  @Test
  void parseReturnsNullForInvalidInput() {
    assertThat(SourceFormat.JSON.parse("{invalid-json")).isNull();
    assertThat(SourceFormat.JSON.parse("{}")).isNull();
    assertThat(SourceFormat.JSON.parse("{\"a\": 1, \"b\": 2}")).isNull();
    assertThat(SourceFormat.JSON.parse("[{\"trace-sampling\": 0.5}, {}]")).isNull();
    assertThat(SourceFormat.KEYVALUE.parse("not-key-value")).isNull();
  }

  @Test
  void parseReturnsImmutableListsAcrossFormats() {
    List<SourceWrapper> jsonParsed = SourceFormat.JSON.parse("{\"trace-sampling\": 0.5}");
    List<SourceWrapper> keyValueParsed = SourceFormat.KEYVALUE.parse("trace-sampling=0.5");

    assertThatThrownBy(() -> jsonParsed.add(parsed("other-policy", "1.0")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> keyValueParsed.add(parsed("other-policy", "1.0")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void parseRejectsNullInputAcrossFormats() {
    assertThatThrownBy(() -> SourceFormat.JSON.parse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
    assertThatThrownBy(() -> SourceFormat.KEYVALUE.parse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }

  private static SourceWrapper parsed(String key, String value) {
    return new KeyValueSourceWrapper(key, value);
  }
}
