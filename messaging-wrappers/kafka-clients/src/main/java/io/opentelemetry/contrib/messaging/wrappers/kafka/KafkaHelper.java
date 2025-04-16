/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaProcessRequest;

public final class KafkaHelper {

  public static <REQUEST extends KafkaProcessRequest>
      KafkaProcessWrapperBuilder<REQUEST> processWrapperBuilder() {
    return new KafkaProcessWrapperBuilder<>();
  }

  private KafkaHelper() {}
}
