/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttributesMapperTest {

  @Test
  void verifyMapping() {
    Attributes attributes =
        Attributes.builder()
            .put(AttributeKey.stringKey("someString"), "someValue")
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

  private static Attributes mapFromProto(List<KeyValue> keyValues) {
    return AttributesMapper.getInstance().protoToAttributes(keyValues);
  }
}
