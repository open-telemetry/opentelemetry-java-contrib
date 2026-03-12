/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeyValueSourceWrapperTest {

  @Test
  void parseSupportsSingleKeyValue() {
    List<SourceWrapper> parsed = KeyValueSourceWrapper.parse("trace-sampling=0.5");

    assertThat(parsed).hasSize(1);
    KeyValueSourceWrapper wrapper = (KeyValueSourceWrapper) parsed.get(0);
    assertThat(wrapper.getFormat()).isEqualTo(SourceFormat.KEYVALUE);
    assertThat(wrapper.getPolicyType()).isEqualTo("trace-sampling");
    assertThat(wrapper.getKey()).isEqualTo("trace-sampling");
    assertThat(wrapper.getValue()).isEqualTo("0.5");
  }

  @Test
  void parseSupportsMultipleLinesAndSkipsBlanks() {
    String source = "\ntrace-sampling=0.5\r\nother-policy=1.0\n   \n";

    List<SourceWrapper> parsed = KeyValueSourceWrapper.parse(source);

    assertThat(parsed).hasSize(2);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
    assertThat(parsed.get(1).getPolicyType()).isEqualTo("other-policy");
  }

  @Test
  void parseSupportsEmptyInput() {
    assertThat(KeyValueSourceWrapper.parse("")).isEmpty();
    assertThat(KeyValueSourceWrapper.parse("\n   \r\n")).isEmpty();
  }

  @Test
  void parseResultIsImmutable() {
    List<SourceWrapper> parsed = KeyValueSourceWrapper.parse("trace-sampling=0.5");

    assertThatThrownBy(() -> parsed.add(new KeyValueSourceWrapper("other-policy", "1.0")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void parseReturnsNullForInvalidInput() {
    assertThat(KeyValueSourceWrapper.parse("missing-separator")).isNull();
    assertThat(KeyValueSourceWrapper.parse("=value")).isNull();
    assertThat(KeyValueSourceWrapper.parse("# comment\ntrace-sampling=0.5")).isNull();
  }

  @Test
  void parseRejectsNullInput() {
    assertThatThrownBy(() -> KeyValueSourceWrapper.parse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }

  @Test
  void parseTrimsKeyAndValue() {
    List<SourceWrapper> parsed = KeyValueSourceWrapper.parse("  trace-sampling  = 0.25 ");

    assertThat(parsed).hasSize(1);
    KeyValueSourceWrapper wrapper = (KeyValueSourceWrapper) parsed.get(0);
    assertThat(wrapper.getKey()).isEqualTo("trace-sampling");
    assertThat(wrapper.getValue()).isEqualTo("0.25");
  }
}
