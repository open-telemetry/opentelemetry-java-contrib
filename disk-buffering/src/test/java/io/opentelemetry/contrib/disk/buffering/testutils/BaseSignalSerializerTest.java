/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.DelimitedProtoStreamReader;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.StreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class BaseSignalSerializerTest<SIGNAL_SDK_ITEM> {
  protected byte[] serialize(SIGNAL_SDK_ITEM... items) {
    return getSerializer().serialize(Arrays.asList(items));
  }

  protected List<SIGNAL_SDK_ITEM> deserialize(byte[] source) {
    try (ByteArrayInputStream in = new ByteArrayInputStream(source)) {
      StreamReader streamReader = DelimitedProtoStreamReader.Factory.getInstance().create(in);
      return getDeserializer().deserialize(Objects.requireNonNull(streamReader.read()).content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void assertSerialization(SIGNAL_SDK_ITEM... targets) {
    byte[] serialized = serialize(targets);
    assertThat(deserialize(serialized)).containsExactly(targets);
  }

  protected void assertSerializeDeserialize(SIGNAL_SDK_ITEM input, SIGNAL_SDK_ITEM expected) {
    byte[] serialized = serialize(input);
    assertThat(deserialize(serialized)).containsExactly(expected);
  }

  protected abstract SignalSerializer<SIGNAL_SDK_ITEM> getSerializer();

  protected abstract SignalDeserializer<SIGNAL_SDK_ITEM> getDeserializer();
}
