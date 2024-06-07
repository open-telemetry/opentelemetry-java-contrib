/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.pooling;

/**
 * Defines a pooled object factory
 *
 * @param <T> pooled object type
 */
public interface Allocator<T> {

  /** Creates a new instance of pooled object type. */
  T createInstance();
}
