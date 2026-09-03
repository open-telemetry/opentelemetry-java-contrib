/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class JsonSourceWrapperTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Set<String> MAPPED_POLICY_IDS =
      new HashSet<>(Arrays.asList("trace-sampling", "metric-exporter-stop", "other-policy"));

  @Test
  void parseSupportsSingleObject() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse("{\"trace-sampling\": 0.5}", MAPPED_POLICY_IDS);

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0)).isInstanceOf(JsonSourceWrapper.class);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
  }

  @Test
  void parseRejectsMultiPolicyObject() {
    assertThat(
            JsonSourceWrapper.parse(
                "{\"trace-sampling\": 0.5, \"other-policy\": true}", MAPPED_POLICY_IDS))
        .isEmpty();
  }

  @Test
  void parseLogsDroppedObjectsAtDebugLevel() {
    Logger logger = Logger.getLogger(JsonSourceWrapper.class.getName());
    Level previousLevel = logger.getLevel();
    List<LogRecord> records = new ArrayList<>();
    Handler handler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            records.add(record);
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    handler.setLevel(Level.FINE);
    logger.setLevel(Level.FINE);
    logger.addHandler(handler);
    try {
      JsonSourceWrapper.parse("{\"trace-sampling\":0.5,\"other-policy\":true}", MAPPED_POLICY_IDS);
      JsonSourceWrapper.parse(
          "[{\"trace-sampling\":0.5},{\"unmapped\":true},42]", MAPPED_POLICY_IDS);
    } finally {
      logger.removeHandler(handler);
      logger.setLevel(previousLevel);
    }

    assertThat(records).extracting(LogRecord::getLevel).containsOnly(Level.FINE);
    assertThat(records)
        .extracting(LogRecord::getMessage)
        .containsExactly(
            "Ignoring invalid or unmapped JSON policy object: "
                + "{\"trace-sampling\":0.5,\"other-policy\":true}",
            "Ignoring invalid or unmapped JSON policy object: {\"unmapped\":true}");
  }

  @Test
  void parseKeepsFullPolicyObjectTogether() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse(fullTraceSamplingPolicy(), MAPPED_POLICY_IDS);

    assertThat(parsed).hasSize(1);
    assertThat(parsed.get(0).getPolicyType()).isEqualTo("trace-sampling");
    assertThat(((JsonSourceWrapper) parsed.get(0)).asJsonNode().get("name").asText())
        .isEqualTo("Trace sampling rate");
  }

  @Test
  void parseValidatesCommonFullPolicyEnvelope() {
    assertThat(
            JsonSourceWrapper.parse(
                "{\"id\":\"trace-sampling\",\"trace\":{\"match\":[{\"exists\":true}],"
                    + "\"keep\":{\"probability\":0.1}}}",
                MAPPED_POLICY_IDS))
        .isEmpty();
    assertThat(
            JsonSourceWrapper.parse(
                "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling rate\","
                    + "\"trace\":{\"match\":[],\"keep\":{\"probability\":0.1}}}",
                MAPPED_POLICY_IDS))
        .isEmpty();
    assertThat(
            JsonSourceWrapper.parse(
                "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling rate\","
                    + "\"trace\":{\"match\":[{}],\"keep\":{\"probability\":0.1}}}",
                MAPPED_POLICY_IDS))
        .isEmpty();
    assertThat(
            JsonSourceWrapper.parse(
                "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling rate\","
                    + "\"trace\":{\"match\":[{\"exists\":true}]}}",
                MAPPED_POLICY_IDS))
        .isEmpty();
    assertThat(
            JsonSourceWrapper.parse(
                "{\"id\":\"trace-sampling\",\"name\":\"Trace sampling rate\","
                    + "\"trace\":{\"match\":[{\"exists\":true}],\"keep\":{\"probability\":0.1}},"
                    + "\"metric\":{\"match\":[{\"exists\":true}],\"keep\":false}}",
                MAPPED_POLICY_IDS))
        .isEmpty();
  }

  @Test
  void parseAndUnwrapFullPolicyWithoutPolicySpecificKnowledge() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse(
            "{\"id\":\"metric-exporter-stop\",\"name\":\"Stop metric export\","
                + "\"metric\":{\"match\":[{\"metric_field\":\"name\",\"exists\":true}],"
                + "\"keep\":false}}",
            MAPPED_POLICY_IDS);

    assertThat(parsed).hasSize(1);
    JsonSourceWrapper policy = (JsonSourceWrapper) parsed.get(0);
    assertThat(policy.getPolicyType()).isEqualTo("metric-exporter-stop");
    assertThat(policy.getPolicyValue().isBoolean()).isTrue();
    assertThat(policy.getPolicyValue().asBoolean()).isFalse();
  }

  @Test
  void getPolicyValueUnwrapsKeyedAndFullPolicies() throws Exception {
    JsonSourceWrapper keyed = new JsonSourceWrapper(MAPPER.readTree("{\"trace-sampling\":0.25}"));
    JsonSourceWrapper full = new JsonSourceWrapper(MAPPER.readTree(fullTraceSamplingPolicy()));

    assertThat(keyed.getPolicyValue().asDouble()).isEqualTo(0.25);
    assertThat(full.getPolicyValue().get("probability").asDouble()).isEqualTo(0.1);
  }

  @Test
  void withPolicyTypeRemapsKeyedAndFullPoliciesWithoutMutatingSources() throws Exception {
    JsonSourceWrapper keyed = new JsonSourceWrapper(MAPPER.readTree("{\"external\":0.25}"));
    JsonSourceWrapper full = new JsonSourceWrapper(MAPPER.readTree(fullTraceSamplingPolicy()));

    JsonSourceWrapper remappedKeyed = (JsonSourceWrapper) keyed.withPolicyType("trace-sampling");
    JsonSourceWrapper remappedFull = (JsonSourceWrapper) full.withPolicyType("mapped-trace");

    assertThat(remappedKeyed.getPolicyType()).isEqualTo("trace-sampling");
    assertThat(remappedKeyed.getPolicyValue().asDouble()).isEqualTo(0.25);
    assertThat(keyed.getPolicyType()).isEqualTo("external");
    assertThat(remappedFull.getPolicyType()).isEqualTo("mapped-trace");
    assertThat(full.getPolicyType()).isEqualTo("trace-sampling");
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
  void parseSupportsArrayMixingKeyedAndFullPolicyObjects() {
    List<SourceWrapper> parsed =
        JsonSourceWrapper.parse(
            "[{\"other-policy\":1}," + fullTraceSamplingPolicy() + "]", MAPPED_POLICY_IDS);

    assertThat(parsed)
        .extracting(SourceWrapper::getPolicyType)
        .containsExactly("other-policy", "trace-sampling");
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
  void getPolicyTypeUsesFullPolicyId() throws Exception {
    JsonSourceWrapper wrapper = new JsonSourceWrapper(MAPPER.readTree(fullTraceSamplingPolicy()));

    assertThat(wrapper.getPolicyType()).isEqualTo("trace-sampling");
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
        .isEmpty();
    assertThat(
            JsonSourceWrapper.parse(
                "{\"id\":\"unmapped\",\"trace-sampling\":0.5}", MAPPED_POLICY_IDS))
        .isEmpty();
  }

  @Test
  void isSingleKeyObjectRecognizesRawShape() {
    assertThat(JsonSourceWrapper.isSingleKeyObject("{\"trace-sampling\": 0.5}")).isTrue();
    assertThat(JsonSourceWrapper.isSingleKeyObject("{\"trace-sampling\": 0.5, \"typo\": 1}"))
        .isFalse();
    assertThat(JsonSourceWrapper.isSingleKeyObject("{}")).isFalse();
    assertThat(JsonSourceWrapper.isSingleKeyObject("[{\"trace-sampling\": 0.5}]")).isFalse();
    assertThat(JsonSourceWrapper.isSingleKeyObject("{invalid-json")).isFalse();
  }

  @Test
  void isSinglePolicyObjectRecognizesKeyedAndFullShapes() {
    assertThat(JsonSourceWrapper.isSinglePolicyObject("{\"trace-sampling\":0.5}")).isTrue();
    assertThat(JsonSourceWrapper.isSinglePolicyObject(fullTraceSamplingPolicy())).isTrue();
    assertThat(
            JsonSourceWrapper.isSinglePolicyObject(
                "{\"trace-sampling\":0.5,\"other-policy\":true}"))
        .isFalse();
    assertThat(JsonSourceWrapper.isSinglePolicyObject("{\"id\":\" \"}")).isFalse();
    assertThat(JsonSourceWrapper.isSinglePolicyObject("[]")).isFalse();
  }

  @Test
  void parseRejectsNullInput() {
    assertThatThrownBy(() -> JsonSourceWrapper.parse(null, emptySet()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }

  @Test
  void isSingleKeyObjectRejectsNullInput() {
    assertThatThrownBy(() -> JsonSourceWrapper.isSingleKeyObject(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }

  @Test
  void isSinglePolicyObjectRejectsNullInput() {
    assertThatThrownBy(() -> JsonSourceWrapper.isSinglePolicyObject(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("source cannot be null");
  }

  private static String fullTraceSamplingPolicy() {
    return "{"
        + "\"id\":\"trace-sampling\","
        + "\"name\":\"Trace sampling rate\","
        + "\"description\":\"Set the global trace sampling rate to 10%.\","
        + "\"enabled\":true,"
        + "\"created_at_unix_nano\":\"1718890000000000000\","
        + "\"modified_at_unix_nano\":\"1718893600000000000\","
        + "\"labels\":[{\"key\":\"policy.scope\","
        + "\"value\":{\"string_value\":\"global\"}}],"
        + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true,"
        + "\"negate\":false,\"case_insensitive\":false}],"
        + "\"keep\":{\"probability\":0.1,\"mode\":\"proportional\","
        + "\"sampling_precision\":6,\"hash_seed\":0,\"fail_closed\":false}}"
        + "}";
  }
}
