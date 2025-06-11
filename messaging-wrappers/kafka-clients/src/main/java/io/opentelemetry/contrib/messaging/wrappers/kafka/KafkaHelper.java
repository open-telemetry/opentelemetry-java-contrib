/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.kafka;

public final class KafkaHelper {

  public static KafkaProcessWrapperBuilder processWrapperBuilder() {
    return new KafkaProcessWrapperBuilder();
  }

  private KafkaHelper() {}
}
