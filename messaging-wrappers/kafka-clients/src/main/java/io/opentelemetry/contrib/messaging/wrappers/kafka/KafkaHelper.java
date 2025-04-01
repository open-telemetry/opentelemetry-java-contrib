package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaProcessRequest;

public final class KafkaHelper {

  public static <REQUEST extends KafkaProcessRequest> KafkaProcessWrapperBuilder<REQUEST> processWrapperBuilder() {
    return new KafkaProcessWrapperBuilder<>();
  }

  private KafkaHelper() {
  }
}
