/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.pooling;

import org.jctools.queues.MpmcArrayQueue;

/**
 * Object pool
 *
 * @param <T> pooled object type. Does not have to implement {@link Recyclable} in order to allow
 *     for dealing with objects that are outside of elastic apm agent (like standard JDK or third
 *     party library classes).
 */
public interface ObjectPool<T> {

  /**
   * Tries to reuse any existing instance if pool has any, otherwise creates a new un-pooled
   * instance
   *
   * @return object instance, either from pool or freshly allocated
   */
  T createInstance();

  /**
   * Recycles an object
   *
   * @param obj object to recycle
   */
  void recycle(T obj);

  void clear();

  public static <T extends Recyclable> ObjectPool<T> createRecyclable(
      int capacity, Allocator<T> allocator) {
    return QueueBasedObjectPool.ofRecyclable(
        new MpmcArrayQueue<>(capacity), /* preAllocate= */ false, allocator);
  }
}
