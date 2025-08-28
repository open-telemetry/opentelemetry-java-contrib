/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    assertThat(actual).isNotNull();
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

    assertThat(actual).isNotNull();
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

    assertThatThrownBy(
            () ->
                new KafkaSpanExporterBuilder()
                    .setProducer(
                        KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                            .setConfig(producerConfig)
                            .build())
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithMissingProducer() {
    assertThatThrownBy(() -> new KafkaSpanExporterBuilder().setTopicName("a-topic").build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithMissingProducerConfig() {
    assertThatThrownBy(
            () ->
                new KafkaSpanExporterBuilder()
                    .setTopicName("a-topic")
                    .setProducer(KafkaSpanExporterBuilder.ProducerBuilder.newInstance().build())
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithMissingSerializers() {
    ImmutableMap<String, Object> producerConfig =
        ImmutableMap.of(
            BOOTSTRAP_SERVERS_CONFIG,
            "PLAINTEXT://localhost:123",
            ProducerConfig.CLIENT_ID_CONFIG,
            "some clientId");

    assertThatThrownBy(
            () ->
                new KafkaSpanExporterBuilder()
                    .setTopicName("a-topic")
                    .setProducer(
                        KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                            .setConfig(producerConfig)
                            .build())
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
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

    assertThatThrownBy(
            () ->
                new KafkaSpanExporterBuilder()
                    .setTopicName("a-topic")
                    .setProducer(
                        KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                            .setConfig(producerConfig)
                            .setValueSerializer(valueSerializerMock)
                            .build())
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
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

    assertThatThrownBy(
            () ->
                new KafkaSpanExporterBuilder()
                    .setTopicName("a-topic")
                    .setProducer(
                        KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                            .setConfig(producerConfig)
                            .setKeySerializer(keySerializerMock)
                            .build())
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
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

    assertThatThrownBy(
            () ->
                new KafkaSpanExporterBuilder()
                    .setTopicName("a-topic")
                    .setProducer(
                        KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
                            .setConfig(producerConfig)
                            .setKeySerializer(keySerializerMock)
                            .setValueSerializer(valueSerializerMock)
                            .build())
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
  }
}
