/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SourceFormatTest {

  @Test
  void configValuesAreStable() {
    assertThat(SourceFormat.JSONKEYVALUE.configValue()).isEqualTo("jsonkeyvalue");
    assertThat(SourceFormat.KEYVALUE.configValue()).isEqualTo("keyvalue");
  }

  @Test
  void fromConfigValueParsesCaseInsensitive() {
    assertThat(SourceFormat.fromConfigValue("KEYVALUE")).isEqualTo(SourceFormat.KEYVALUE);
    assertThat(SourceFormat.fromConfigValue("JsonKeyValue")).isEqualTo(SourceFormat.JSONKEYVALUE);
  }

  @Test
  void fromConfigValueTrimsWhitespace() {
    assertThat(SourceFormat.fromConfigValue("  keyvalue  ")).isEqualTo(SourceFormat.KEYVALUE);
  }

  @Test
  void fromConfigValueRejectsNullAndUnknown() {
    assertThatThrownBy(() -> SourceFormat.fromConfigValue(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value cannot be null");
    assertThatThrownBy(() -> SourceFormat.fromConfigValue("other"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown source format (normalized): 'other' (original: 'other')");
  }

  @Test
  void parseDelegatesToJsonParser() {
    List<SourceWrapper> parsed =
        SourceFormat.JSONKEYVALUE.parse("{\"trace-sampling\": 0.5}", singleton("trace-sampling"));

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(JsonSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseDelegatesToKeyValueParser() {
    List<SourceWrapper> parsed = SourceFormat.KEYVALUE.parse("trace-sampling=0.5", emptySet());

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(KeyValueSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseSkipsJsonObjectWithUnmappedPolicyId() {
    assertThat(SourceFormat.JSONKEYVALUE.parse("{\"unmapped\": 0.5}", singleton("sampling_rate")))
        .isEmpty();
  }

  @Test
  void parseUsesStandardParserForKeyValue() {
    List<SourceWrapper> parsed =
        SourceFormat.KEYVALUE.parse("sampling_rate=0.5", singleton("sampling_rate"));

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("sampling_rate");
  }

  @Test
  void parseSupportsEmptyInputAcrossFormats() {
    assertThat(SourceFormat.JSONKEYVALUE.parse("[]", emptySet())).isEmpty();
    assertThat(SourceFormat.KEYVALUE.parse("", emptySet())).isEmpty();
    assertThat(SourceFormat.KEYVALUE.parse("\n   \r\n", emptySet())).isEmpty();
  }

  @Test
  void parseReturnsNullForInvalidInput() {
    assertThat(SourceFormat.JSONKEYVALUE.parse("{invalid-json", emptySet())).isNull();
    assertThat(SourceFormat.JSONKEYVALUE.parse("{}", emptySet())).isEmpty();
    assertThat(SourceFormat.JSONKEYVALUE.parse("{\"a\": 1, \"b\": 2}", singleton("a")))
        .extracting(SourceWrapper::getPolicyType)
        .containsExactly("a");
    assertThat(
            SourceFormat.JSONKEYVALUE.parse(
                "[{\"trace-sampling\": 0.5}, {}]", singleton("trace-sampling")))
        .extracting(SourceWrapper::getPolicyType)
        .containsExactly("trace-sampling");
    assertThat(SourceFormat.KEYVALUE.parse("not-key-value", emptySet())).isNull();
  }

  @Test
  void parseReturnsImmutableListsAcrossFormats() {
    List<SourceWrapper> jsonParsed =
        SourceFormat.JSONKEYVALUE.parse("{\"trace-sampling\": 0.5}", singleton("trace-sampling"));
    List<SourceWrapper> keyValueParsed =
        SourceFormat.KEYVALUE.parse("trace-sampling=0.5", emptySet());

    assertThatThrownBy(() -> jsonParsed.add(parsed("other-policy", "1.0")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> keyValueParsed.add(parsed("other-policy", "1.0")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void parseRejectsNullInputAcrossFormats() {
    assertThatThrownBy(() -> SourceFormat.JSONKEYVALUE.parse(null, emptySet()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
    assertThatThrownBy(() -> SourceFormat.KEYVALUE.parse(null, emptySet()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }

  private static SourceWrapper parsed(String key, String value) {
    return new KeyValueSourceWrapper(key, value);
  }
}
