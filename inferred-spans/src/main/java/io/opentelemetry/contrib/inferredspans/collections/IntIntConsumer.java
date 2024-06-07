/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.collections;

/** This is an (int, int) primitive specialisation of a BiConsumer */
@FunctionalInterface
public interface IntIntConsumer {
  /**
   * Accept two values that comes as a tuple of ints.
   *
   * @param valueOne for the tuple.
   * @param valueTwo for the tuple.
   */
  void accept(int valueOne, int valueTwo);
}
