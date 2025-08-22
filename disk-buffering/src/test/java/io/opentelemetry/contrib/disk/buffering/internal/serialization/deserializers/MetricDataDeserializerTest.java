/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.testutils.BaseSignalSerializerTest;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import org.junit.jupiter.api.Test;

class MetricDataDeserializerTest extends BaseSignalSerializerTest<MetricData> {

  @Test
  void whenDecodingMalformedMessage_wrapIntoDeserializationException() {
    assertThrows(
        DeserializationException.class,
        () -> getDeserializer().deserialize(TestData.makeMalformedSignalBinary()));
  }

  @Test
  void whenDecodingTooShortMessage_wrapIntoDeserializationException() {
    assertThrows(
        DeserializationException.class,
        () -> getDeserializer().deserialize(TestData.makeTooShortSignalBinary()));
  }

  @Override
  protected SignalSerializer<MetricData> getSerializer() {
    return SignalSerializer.ofMetrics();
  }

  @Override
  protected SignalDeserializer<MetricData> getDeserializer() {
    return SignalDeserializer.ofMetrics();
  }
}
