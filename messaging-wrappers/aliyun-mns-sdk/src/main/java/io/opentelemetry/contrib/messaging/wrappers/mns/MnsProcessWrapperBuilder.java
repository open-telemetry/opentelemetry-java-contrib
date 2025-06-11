/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns;

import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapperBuilder;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MnsConsumerAttributesGetter;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MnsProcessRequest;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import java.util.ArrayList;

public class MnsProcessWrapperBuilder extends MessagingProcessWrapperBuilder<MnsProcessRequest> {

  MnsProcessWrapperBuilder() {
    super();
    super.textMapGetter = MnsTextMapGetter.create();
    super.spanNameExtractor =
        MessagingSpanNameExtractor.create(
            MnsConsumerAttributesGetter.INSTANCE, MessageOperation.PROCESS);
    super.attributesExtractors = new ArrayList<>();
    super.attributesExtractors.add(
        MessagingAttributesExtractor.create(
            MnsConsumerAttributesGetter.INSTANCE, MessageOperation.PROCESS));
  }
}
