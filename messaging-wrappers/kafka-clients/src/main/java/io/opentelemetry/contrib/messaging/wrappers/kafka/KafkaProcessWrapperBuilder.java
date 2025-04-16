/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.contrib.messaging.wrappers.DefaultMessagingProcessWrapperBuilder;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaProcessRequest;

public class KafkaProcessWrapperBuilder<REQUEST extends KafkaProcessRequest>
    extends DefaultMessagingProcessWrapperBuilder<REQUEST> {

  KafkaProcessWrapperBuilder() {
    super();
    super.textMapGetter = KafkaTextMapGetter.create();
  }
}
