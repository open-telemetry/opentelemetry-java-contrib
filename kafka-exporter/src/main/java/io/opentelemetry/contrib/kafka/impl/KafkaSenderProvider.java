/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.impl;

import io.opentelemetry.exporter.internal.marshal.Marshaler;

/**
 * A service provider interface (SPI) for providing {@link KafkaSender}s backed by different client
 * libraries.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class KafkaSenderProvider {

  /** Returns a {@link KafkaSender} configured with the provided parameters. */
  @SuppressWarnings("TooManyParameters")
  public <T extends Marshaler> KafkaSender<T> createSender(
      String brokers, String topic, long timeout) {
    return new KafkaSender<T>(brokers, topic, timeout);
  }
}
