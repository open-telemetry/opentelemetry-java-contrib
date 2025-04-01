package io.opentelemetry.contrib.messaging.wrappers.kafka.semconv;

import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KafkaProcessRequest implements MessagingProcessRequest {

  private final ConsumerRecord<?, ?> consumerRecord;

  private final String clientId;

  private final String consumerGroup;

  public static KafkaProcessRequest of(ConsumerRecord<?, ?> consumerRecord) {
    return of(consumerRecord, null, null);
  }

  public static KafkaProcessRequest of(ConsumerRecord<?, ?> consumerRecord, String consumerGroup, String clientId) {
    return new KafkaProcessRequest(consumerRecord, consumerGroup, clientId);
  }

  @Override
  public String getSystem() {
    return "kafka";
  }

  @Override
  public String getDestination() {
    if (this.consumerRecord == null) {
      return null;
    }
    return this.consumerRecord.topic();
  }

  @Nullable
  @Override
  public String getDestinationTemplate() {
    return null;
  }

  @Override
  public boolean isTemporaryDestination() {
    return false;
  }

  @Override
  public boolean isAnonymousDestination() {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId() {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize() {
    if (this.consumerRecord == null) {
      return null;
    }
    long size = this.consumerRecord.serializedValueSize();
    return size >= 0 ? size : null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize() {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId() {
    return null;
  }

  @Nullable
  @Override
  public String getClientId() {
    return this.clientId;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount() {
    return null;
  }

  @Override
  public List<String> getMessageHeader(String name) {
    if (this.consumerRecord == null) {
      return Collections.emptyList();
    }
    return StreamSupport.stream(this.consumerRecord.headers().headers(name).spliterator(), false)
        .map(header -> new String(header.value(), StandardCharsets.UTF_8))
        .collect(Collectors.toList());
  }

  @Nullable
  public String getConsumerGroup() {
    return this.consumerGroup;
  }

  public ConsumerRecord<?, ?> getRecord() {
    return consumerRecord;
  }

  private KafkaProcessRequest(ConsumerRecord<?, ?> consumerRecord, String consumerGroup, String clientId) {
    this.consumerRecord = consumerRecord;
    this.consumerGroup = consumerGroup;
    this.clientId = clientId;
  }
}
