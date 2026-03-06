/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonSourceWrapperTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void parseSupportsSingleObject() {
    List<SourceWrapper> parsed = JsonSourceWrapper.parse("{\"trace-sampling\": 0.5}");

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(JsonSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseSupportsArrayOfObjects() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse("[{\"other-policy\": 1}, {\"trace-sampling\": 0.5}]");

    assertThat(parsed).hasSize(2);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("other-policy");
    assertThat(parsed.get(1).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseSupportsEmptyArray() {
    assertThat(JsonSourceWrapper.parse("[]")).isEmpty();
  }

  @Test
  void parseArrayResultIsImmutable() {
    List<SourceWrapper> parsed = JsonSourceWrapper.parse("[{\"trace-sampling\": 0.5}]");

    assertThatThrownBy(() -> parsed.add(new JsonSourceWrapper(MAPPER.readTree("{\"x\":1}"))))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void getPolicyTypeReturnsNullWhenObjectHasMultipleFields() throws Exception {
    JsonSourceWrapper wrapper = new JsonSourceWrapper(MAPPER.readTree("{\"a\": 1, \"b\": 2}"));

    assertThat(wrapper.getPolicyType()).isNull();
  }

  @Test
  void parseRejectsUnsupportedJsonShapes() {
    assertThat(JsonSourceWrapper.parse("{}")).isNull();
    assertThat(JsonSourceWrapper.parse("{\"a\": 1, \"b\": 2}")).isNull();
    assertThat(JsonSourceWrapper.parse("[1, 2, 3]")).isNull();
    assertThat(JsonSourceWrapper.parse("[{\"trace-sampling\": 0.5}, {}]")).isNull();
    assertThat(JsonSourceWrapper.parse("[{\"a\": 1, \"b\": 2}]")).isNull();
    assertThat(JsonSourceWrapper.parse("\"text\"")).isNull();
    assertThat(JsonSourceWrapper.parse("{invalid-json")).isNull();
  }

  @Test
  void parseRejectsNullInput() {
    assertThatThrownBy(() -> JsonSourceWrapper.parse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }
}
