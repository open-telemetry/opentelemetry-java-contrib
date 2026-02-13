/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AttributesMapperTest {

  @Test
  void verifyMapping() {
    Attributes attributes =
        Attributes.builder()
            .put(AttributeKey.stringKey("someString"), "someValue")
            .put(AttributeKey.stringKey("emptyString"), "")
            .put(AttributeKey.booleanKey("someBool"), true)
            .put(AttributeKey.longKey("someLong"), 10L)
            .put(AttributeKey.doubleKey("someDouble"), 10.0)
            .build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyArrayMapping() {
    Attributes attributes =
        Attributes.builder()
            .put(
                AttributeKey.stringArrayKey("someString"),
                Arrays.asList("firstString", "secondString"))
            .put(AttributeKey.booleanArrayKey("someBool"), Arrays.asList(true, false))
            .put(AttributeKey.longArrayKey("someLong"), Arrays.asList(10L, 50L))
            .put(AttributeKey.doubleArrayKey("someDouble"), Arrays.asList(10.0, 50.5))
            .build();

    List<KeyValue> serialized = mapToProto(attributes);

    assertThat(mapFromProto(serialized)).isEqualTo(attributes);
  }

  private static List<KeyValue> mapToProto(Attributes attributes) {
    return AttributesMapper.getInstance().attributesToProto(attributes);
  }

  @Test
  void verifyValueTypeMapping_Primitives() {
    Attributes attributes =
        Attributes.builder()
            .put(AttributeKey.valueKey("stringValue"), Value.of("hello"))
            .put(AttributeKey.valueKey("boolValue"), Value.of(true))
            .put(AttributeKey.valueKey("longValue"), Value.of(42L))
            .put(AttributeKey.valueKey("doubleValue"), Value.of(3.14))
            .build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyValueTypeMapping_Bytes() {
    byte[] testBytes = "hello world".getBytes(StandardCharsets.UTF_8);
    Attributes attributes =
        Attributes.builder().put(AttributeKey.valueKey("bytesValue"), Value.of(testBytes)).build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyValueTypeMapping_Array() {
    Attributes attributes =
        Attributes.builder()
            .put(
                AttributeKey.valueKey("arrayValue"),
                Value.of(Value.of("str"), Value.of(123L), Value.of(true)))
            .build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyValueTypeMapping_KeyValueList() {
    Map<String, Value<?>> map = new LinkedHashMap<>();
    map.put("key1", Value.of("value1"));
    map.put("key2", Value.of(42L));
    map.put("key3", Value.of(true));

    Attributes attributes =
        Attributes.builder().put(AttributeKey.valueKey("mapValue"), Value.of(map)).build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyValueTypeMapping_NestedStructures() {
    Map<String, Value<?>> innerMap = new LinkedHashMap<>();
    innerMap.put("nested", Value.of("value"));

    Map<String, Value<?>> outerMap = new LinkedHashMap<>();
    outerMap.put("level1", Value.of(innerMap));
    outerMap.put("array", Value.of(Value.of(1L), Value.of(2L), Value.of(3L)));

    Attributes attributes =
        Attributes.builder().put(AttributeKey.valueKey("complex"), Value.of(outerMap)).build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyValueTypeMapping_Empty() {
    Attributes attributes =
        Attributes.builder().put(AttributeKey.valueKey("emptyValue"), Value.empty()).build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  @Test
  void verifyValueTypeMapping_EmptyCollections() {
    Attributes attributes =
        Attributes.builder()
            .put(AttributeKey.valueKey("emptyArray"), Value.of(Collections.emptyList()))
            .put(AttributeKey.valueKey("emptyMap"), Value.of(Collections.emptyMap()))
            .build();

    List<KeyValue> proto = mapToProto(attributes);

    assertThat(mapFromProto(proto)).isEqualTo(attributes);
  }

  private static Attributes mapFromProto(List<KeyValue> keyValues) {
    return AttributesMapper.getInstance().protoToAttributes(keyValues);
  }
}
