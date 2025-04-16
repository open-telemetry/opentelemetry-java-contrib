/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaProcessRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.kafka.common.header.Header;

/**
 * Copied from <a
 * href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/kafka/kafka-clients/kafka-clients-common-0.11/library/src/main/java/io/opentelemetry/instrumentation/kafkaclients/common/v0_11/internal/KafkaConsumerRecordGetter.java>KafkaConsumerRecordGetter</a>.
 */
public class KafkaTextMapGetter<REQUEST extends KafkaProcessRequest>
    implements TextMapGetter<REQUEST> {

  public static <REQUEST extends KafkaProcessRequest> TextMapGetter<REQUEST> create() {
    return new KafkaTextMapGetter<>();
  }

  @Override
  public Iterable<String> keys(@Nullable REQUEST carrier) {
    if (carrier == null || carrier.getRecord() == null) {
      return Collections.emptyList();
    }
    return StreamSupport.stream(carrier.getRecord().headers().spliterator(), false)
        .map(Header::key)
        .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public String get(@Nullable REQUEST carrier, String key) {
    if (carrier == null || carrier.getRecord() == null) {
      return null;
    }
    Header header = carrier.getRecord().headers().lastHeader(key);
    if (header == null) {
      return null;
    }
    byte[] value = header.value();
    if (value == null) {
      return null;
    }
    return new String(value, StandardCharsets.UTF_8);
  }
}
