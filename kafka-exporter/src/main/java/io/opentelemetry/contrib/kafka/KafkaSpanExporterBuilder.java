/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Serializer;

public class KafkaSpanExporterBuilder {
  private static final long DEFAULT_TIMEOUT_IN_SECONDS = 5L;
  private String topicName;
  private Producer<String, Collection<SpanData>> producer;
  private ExecutorService executorService;
  private long timeoutInSeconds = DEFAULT_TIMEOUT_IN_SECONDS;

  @SuppressWarnings(value = {"NullAway"})
  public KafkaSpanExporterBuilder() {}

  @CanIgnoreReturnValue
  public KafkaSpanExporterBuilder setTopicName(String topicName) {
    this.topicName = topicName;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaSpanExporterBuilder setProducer(Producer<String, Collection<SpanData>> producer) {
    this.producer = producer;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaSpanExporterBuilder setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaSpanExporterBuilder setTimeoutInSeconds(long timeoutInSeconds) {
    this.timeoutInSeconds = timeoutInSeconds;
    return this;
  }

  public KafkaSpanExporter build() {
    if (isNull(topicName)) {
      throw new IllegalArgumentException("topicName cannot be null");
    }
    if (isNull(producer)) {
      throw new IllegalArgumentException("producer cannot be null");
    }
    if (isNull(executorService)) {
      executorService = Executors.newCachedThreadPool();
    }
    return new KafkaSpanExporter(topicName, producer, executorService, timeoutInSeconds);
  }

  public static class ProducerBuilder {
    private Map<String, Object> config;
    private Serializer<String> keySerializer;
    private Serializer<Collection<SpanData>> valueSerializer;

    public static ProducerBuilder newInstance() {
      return new ProducerBuilder();
    }

    @SuppressWarnings(value = {"NullAway"})
    public ProducerBuilder() {}

    @CanIgnoreReturnValue
    public ProducerBuilder setConfig(Map<String, Object> config) {
      this.config = config;
      return this;
    }

    @CanIgnoreReturnValue
    public ProducerBuilder setKeySerializer(Serializer<String> keySerializer) {
      this.keySerializer = keySerializer;
      return this;
    }

    @CanIgnoreReturnValue
    public ProducerBuilder setValueSerializer(Serializer<Collection<SpanData>> valueSerializer) {
      this.valueSerializer = valueSerializer;
      return this;
    }

    public Producer<String, Collection<SpanData>> build() {
      if (isNull(config)) {
        throw new IllegalArgumentException("producer configuration cannot be null");
      }
      boolean correctConfig =
          ((config.containsKey(KEY_SERIALIZER_CLASS_CONFIG)
                      && config.containsKey(VALUE_SERIALIZER_CLASS_CONFIG))
                  ^ (nonNull(keySerializer) && nonNull(valueSerializer)))
              && (config.containsKey(KEY_SERIALIZER_CLASS_CONFIG) ^ nonNull(valueSerializer))
              && (config.containsKey(VALUE_SERIALIZER_CLASS_CONFIG) ^ nonNull(keySerializer));
      if (!correctConfig) {
        throw new IllegalArgumentException(
            "Both the key and value serializers should be provided either in the configuration or by using the corresponding setters");
      }
      if (config.containsKey(KEY_SERIALIZER_CLASS_CONFIG)) {
        return new KafkaProducer<>(config);
      }
      return new KafkaProducer<>(config, keySerializer, valueSerializer);
    }
  }
}
