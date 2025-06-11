/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapperBuilder;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaConsumerAttributesExtractor;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaConsumerAttributesGetter;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaProcessRequest;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import java.util.ArrayList;

public class KafkaProcessWrapperBuilder
    extends MessagingProcessWrapperBuilder<KafkaProcessRequest> {

  KafkaProcessWrapperBuilder() {
    super();
    super.textMapGetter = KafkaTextMapGetter.create();
    super.spanNameExtractor =
        MessagingSpanNameExtractor.create(
            KafkaConsumerAttributesGetter.INSTANCE, MessageOperation.PROCESS);
    super.attributesExtractors = new ArrayList<>();
    super.attributesExtractors.add(
        MessagingAttributesExtractor.create(
            KafkaConsumerAttributesGetter.INSTANCE, MessageOperation.PROCESS));
    super.attributesExtractors.add(KafkaConsumerAttributesExtractor.INSTANCE);
  }
}
