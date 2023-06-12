/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseJsonSerializationTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AttributesJsonConverterTest extends BaseJsonSerializationTest<Attributes> {

  @Test
  public void verifySerialization() {
    Attributes attributes =
        Attributes.builder()
            .put(AttributeKey.stringKey("someString"), "someValue")
            .put(AttributeKey.booleanKey("someBool"), true)
            .put(AttributeKey.longKey("someLong"), 10L)
            .put(AttributeKey.doubleKey("someDouble"), 10.0)
            .build();

    byte[] serialized = serialize(attributes);

    assertEquals(attributes, deserialize(serialized));
  }

  @Test
  public void verifyArraySerialization() {
    Attributes attributes =
        Attributes.builder()
            .put(
                AttributeKey.stringArrayKey("someString"),
                Arrays.asList("firstString", "secondString"))
            .put(AttributeKey.booleanArrayKey("someBool"), Arrays.asList(true, false))
            .put(AttributeKey.longArrayKey("someLong"), Arrays.asList(10L, 50L))
            .put(AttributeKey.doubleArrayKey("someDouble"), Arrays.asList(10.0, 50.5))
            .build();

    byte[] serialized = serialize(attributes);

    assertEquals(attributes, deserialize(serialized));
  }

  @Override
  protected Class<Attributes> getTargetClass() {
    return Attributes.class;
  }
}
