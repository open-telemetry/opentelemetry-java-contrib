package io.opentelemetry.contrib.messaging.wrappers.kafka.semconv;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Copied from <a href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/kafka/kafka-clients/kafka-clients-common-0.11/library/src/main/java/io/opentelemetry/instrumentation/kafkaclients/common/v0_11/internal/KafkaConsumerAttributesExtractor.java>KafkaConsumerAttributesExtractor</a>.
 * */
public final class KafkaConsumerAttributesExtractor<REQUEST extends KafkaProcessRequest>
    implements AttributesExtractor<REQUEST, Void> {

  // copied from MessagingIncubatingAttributes
  private static final AttributeKey<String> MESSAGING_DESTINATION_PARTITION_ID =
      AttributeKey.stringKey("messaging.destination.partition.id");
  private static final AttributeKey<String> MESSAGING_CONSUMER_GROUP_NAME =
      AttributeKey.stringKey("messaging.consumer.group.name");
  private static final AttributeKey<Long> MESSAGING_KAFKA_OFFSET =
      AttributeKey.longKey("messaging.kafka.offset");
  private static final AttributeKey<String> MESSAGING_KAFKA_MESSAGE_KEY =
      AttributeKey.stringKey("messaging.kafka.message.key");
  private static final AttributeKey<Boolean> MESSAGING_KAFKA_MESSAGE_TOMBSTONE =
      AttributeKey.booleanKey("messaging.kafka.message.tombstone");

  public static <REQUEST extends KafkaProcessRequest> AttributesExtractor<REQUEST, Void> create() {
    return new KafkaConsumerAttributesExtractor<>();
  }

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, REQUEST request) {

    ConsumerRecord<?, ?> record = request.getRecord();

    attributes.put(MESSAGING_DESTINATION_PARTITION_ID, String.valueOf(record.partition()));
    attributes.put(MESSAGING_KAFKA_OFFSET, record.offset());

    Object key = record.key();
    if (key != null && canSerialize(key.getClass())) {
      attributes.put(MESSAGING_KAFKA_MESSAGE_KEY, key.toString());
    }
    if (record.value() == null) {
      attributes.put(MESSAGING_KAFKA_MESSAGE_TOMBSTONE, true);
    }

    String consumerGroup = request.getConsumerGroup();
    if (consumerGroup != null) {
      attributes.put(MESSAGING_CONSUMER_GROUP_NAME, consumerGroup);
    }
  }

  private static boolean canSerialize(Class<?> keyClass) {
    // we make a simple assumption here that we can serialize keys by simply calling toString()
    // and that does not work for byte[] or ByteBuffer
    return !(keyClass.isArray() || keyClass == ByteBuffer.class);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable Void unused,
      @Nullable Throwable error) {}

  private KafkaConsumerAttributesExtractor() {}
}