/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaSpanExporterBuilderTest {
  @Mock private Serializer<String> keySerializerMock;
  @Mock private Serializer<Collection<SpanData>> valueSerializerMock;

  @Test
  void buildWithSerializersInSetters() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            ProducerConfig.CLIENT_ID_CONFIG,
            "some clientId");
    KafkaSpanExporter actual =
        new KafkaSpanExporterBuilder()
            .setTopicName("a-topic")
            .setExecutorService(Executors.newCachedThreadPool())
            .setTimeoutInSeconds(2)
            .setProducer(
                KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                    .setConfig(producerConfig)
                    .setKeySerializer(keySerializerMock)
                    .setValueSerializer(valueSerializerMock)
                    .build())
            .build();

    assertNotNull(actual);
    actual.close();
  }

  @Test
  void buildWithSerializersInConfig() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            CLIENT_ID_CONFIG,
            "some clientId",
            KEY_SERIALIZER_CLASS_CONFIG,
            keySerializerMock.getClass().getName(),
            VALUE_SERIALIZER_CLASS_CONFIG,
            valueSerializerMock.getClass().getName());
    KafkaSpanExporter actual =
        new KafkaSpanExporterBuilder()
            .setTopicName("a-topic")
            .setProducer(
                KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                    .setConfig(producerConfig)
                    .build())
            .build();

    assertNotNull(actual);
    actual.close();
  }

  @Test
  void buildWithMissingTopic() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            CLIENT_ID_CONFIG,
            "some clientId",
            KEY_SERIALIZER_CLASS_CONFIG,
            keySerializerMock.getClass().getName(),
            VALUE_SERIALIZER_CLASS_CONFIG,
            valueSerializerMock.getClass().getName());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new KafkaSpanExporterBuilder()
                .setProducer(
                    KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                        .setConfig(producerConfig)
                        .build())
                .build());
  }

  @Test
  void buildWithMissingProducer() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new KafkaSpanExporterBuilder().setTopicName("a-topic").build());
  }

  @Test
  void buildWithMissingProducerConfig() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new KafkaSpanExporterBuilder()
                .setTopicName("a-topic")
                .setProducer(KafkaSpanExporterBuilder.ProducerBuilder.newInstance().build())
                .build());
  }

  @Test
  void buildWithMissingSerializers() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            ProducerConfig.CLIENT_ID_CONFIG,
            "some clientId");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new KafkaSpanExporterBuilder()
                .setTopicName("a-topic")
                .setProducer(
                    KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                        .setConfig(producerConfig)
                        .build())
                .build());
  }

  @Test
  void buildWithKeySerializerInConfigAndValueSerializerInSetter() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            CLIENT_ID_CONFIG,
            "some clientId",
            KEY_SERIALIZER_CLASS_CONFIG,
            keySerializerMock.getClass().getName());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new KafkaSpanExporterBuilder()
                .setTopicName("a-topic")
                .setProducer(
                    KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                        .setConfig(producerConfig)
                        .setValueSerializer(valueSerializerMock)
                        .build())
                .build());
  }

  @Test
  void buildWithValueSerializerInConfigAndKeySerializerInSetter() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            CLIENT_ID_CONFIG,
            "some clientId",
            VALUE_SERIALIZER_CLASS_CONFIG,
            valueSerializerMock.getClass().getName());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new KafkaSpanExporterBuilder()
                .setTopicName("a-topic")
                .setProducer(
                    KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                        .setConfig(producerConfig)
                        .setKeySerializer(keySerializerMock)
                        .build())
                .build());
  }

  @Test
  void buildWithSerializersInConfigAndSetters() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            CLIENT_ID_CONFIG,
            "some clientId",
            KEY_SERIALIZER_CLASS_CONFIG,
            keySerializerMock.getClass().getName(),
            VALUE_SERIALIZER_CLASS_CONFIG,
            valueSerializerMock.getClass().getName());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new KafkaSpanExporterBuilder()
                .setTopicName("a-topic")
                .setProducer(
                    KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                        .setConfig(producerConfig)
                        .setKeySerializer(keySerializerMock)
                        .setValueSerializer(valueSerializerMock)
                        .build())
                .build());
  }
}
