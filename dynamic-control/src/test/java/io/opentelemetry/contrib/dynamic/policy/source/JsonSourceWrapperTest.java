/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JsonSourceWrapperTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Set<String> MAPPED_POLICY_IDS =
      new HashSet<>(Arrays.asList("trace-sampling", "other-policy"));

  @Test
  void parseSupportsSingleObject() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse("{\"trace-sampling\": 0.5}", MAPPED_POLICY_IDS);

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(JsonSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseSupportsMultiPolicyObject() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse(
            "{\"trace-sampling\": 0.5, \"other-policy\": true}", MAPPED_POLICY_IDS);

    assertThat(parsed)
        .extracting(SourceWrapper::getPolicyType)
        .containsExactly("trace-sampling", "other-policy");
  }

  @Test
  void parseSupportsArrayOfObjects() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse(
            "[{\"other-policy\": 1}, {\"trace-sampling\": 0.5}]", MAPPED_POLICY_IDS);

    assertThat(parsed).hasSize(2);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("other-policy");
    assertThat(parsed.get(1).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseSupportsEmptyArray() {
    assertThat(JsonSourceWrapper.parse("[]", emptySet())).isEmpty();
  }

  @Test
  void parseArrayResultIsImmutable() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse("[{\"trace-sampling\": 0.5}]", MAPPED_POLICY_IDS);

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
    assertThat(JsonSourceWrapper.parse("{}", MAPPED_POLICY_IDS)).isEmpty();
    assertThat(JsonSourceWrapper.parse("{\"a\": 1, \"b\": 2}", MAPPED_POLICY_IDS)).isEmpty();
    assertThat(JsonSourceWrapper.parse("[1, 2, 3]", MAPPED_POLICY_IDS)).isEmpty();
    assertThat(JsonSourceWrapper.parse("[{\"trace-sampling\": 0.5}, {}]", MAPPED_POLICY_IDS))
        .extracting(SourceWrapper::getPolicyType)
        .containsExactly("trace-sampling");
    assertThat(
            JsonSourceWrapper.parse(
                "[{\"trace-sampling\": 1, \"other-policy\": 2}]", MAPPED_POLICY_IDS))
        .isEmpty();
    assertThat(JsonSourceWrapper.parse("\"text\"", MAPPED_POLICY_IDS)).isNull();
    assertThat(JsonSourceWrapper.parse("{invalid-json", MAPPED_POLICY_IDS)).isNull();
  }

  @Test
  void parseSkipsUnmappedPolicyIds() {
    assertThat(JsonSourceWrapper.parse("{\"unmapped\": 1}", MAPPED_POLICY_IDS)).isEmpty();
    assertThat(
            JsonSourceWrapper.parse(
                "{\"trace-sampling\": 0.5, \"unmapped\": 1}", MAPPED_POLICY_IDS))
        .extracting(SourceWrapper::getPolicyType)
        .containsExactly("trace-sampling");
  }

  @Test
  void parseRejectsNullInput() {
    assertThatThrownBy(() -> JsonSourceWrapper.parse(null, emptySet()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }
}
