/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.collections;

/** This is an (long, long) primitive specialisation of a BiConsumer */
@FunctionalInterface
public interface LongLongConsumer {
  /**
   * Accept two values that comes as a tuple of longs.
   *
   * @param valueOne for the tuple.
   * @param valueTwo for the tuple.
   */
  void accept(long valueOne, long valueTwo);
}
