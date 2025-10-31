/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka.semconv;

import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaProcessRequest {

  private final ConsumerRecord<?, ?> consumerRecord;

  @Nullable private final String clientId;

  @Nullable private final String consumerGroup;

  public static KafkaProcessRequest of(ConsumerRecord<?, ?> consumerRecord) {
    return of(consumerRecord, null, null);
  }

  public static KafkaProcessRequest of(
      ConsumerRecord<?, ?> consumerRecord,
      @Nullable String consumerGroup,
      @Nullable String clientId) {
    return new KafkaProcessRequest(consumerRecord, consumerGroup, clientId);
  }

  public ConsumerRecord<?, ?> getRecord() {
    return consumerRecord;
  }

  @Nullable
  public String getConsumerGroup() {
    return this.consumerGroup;
  }

  @Nullable
  public String getClientId() {
    return this.clientId;
  }

  private KafkaProcessRequest(
      ConsumerRecord<?, ?> consumerRecord,
      @Nullable String consumerGroup,
      @Nullable String clientId) {
    this.consumerRecord = consumerRecord;
    this.consumerGroup = consumerGroup;
    this.clientId = clientId;
  }
}
