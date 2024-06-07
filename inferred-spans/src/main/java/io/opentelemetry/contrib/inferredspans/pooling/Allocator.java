/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.pooling;

/**
 * Defines pooled object factory
 *
 * @param <T> pooled object type
 */
public interface Allocator<T> {

  /**
   * @return new instance of pooled object type
   */
  T createInstance();
}
