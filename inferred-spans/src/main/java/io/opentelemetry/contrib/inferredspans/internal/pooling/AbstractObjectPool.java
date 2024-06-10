/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.pooling;

import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public abstract class AbstractObjectPool<T> implements ObjectPool<T> {

  protected final Allocator<? extends T> allocator;
  protected final Resetter<? super T> resetter;
  private final AtomicInteger garbageCreated;

  protected AbstractObjectPool(Allocator<? extends T> allocator, Resetter<? super T> resetter) {
    this.allocator = allocator;
    this.resetter = resetter;
    this.garbageCreated = new AtomicInteger();
  }

  @Override
  public final T createInstance() {
    T object = tryCreateInstance();
    if (object == null) {
      // pool does not have available instance, falling back to creating a new one
      object = allocator.createInstance();
    }
    return object;
  }

  @Override
  public final void recycle(T obj) {
    resetter.recycle(obj);
    if (!returnToPool(obj)) {
      // when not able to return object to pool, it means this object will be garbage-collected
      garbageCreated.incrementAndGet();
    }
  }

  public final long getGarbageCreated() {
    return garbageCreated.longValue();
  }

  /**
   * Pushes object reference back into the available pooled instances
   *
   * @param obj recycled object to return to pool
   * @return true if object has been returned to pool, false if pool is already full
   */
  protected abstract boolean returnToPool(T obj);

  /**
   * Tries to create an instance in pool
   *
   * @return {@code null} if pool capacity is exhausted
   */
  @Nullable
  protected abstract T tryCreateInstance();
}
