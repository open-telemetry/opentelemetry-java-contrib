/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import java.util.Arrays;
import java.util.List;

public abstract class BaseSignalSerializerTest<SIGNAL_SDK_ITEM> {
  protected byte[] serialize(SIGNAL_SDK_ITEM... items) {
    return getSerializer().serialize(Arrays.asList(items));
  }

  protected List<SIGNAL_SDK_ITEM> deserialize(byte[] source) {
    return getSerializer().deserialize(source);
  }

  protected void assertSerialization(SIGNAL_SDK_ITEM... targets) {
    byte[] serialized = serialize(targets);
    assertThat(deserialize(serialized)).containsExactly(targets);
  }

  protected abstract SignalSerializer<SIGNAL_SDK_ITEM> getSerializer();
}
