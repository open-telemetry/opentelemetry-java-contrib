/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.pooling;

/**
 * Defines reset strategy to use for a given pooled object type when they are returned to pool
 *
 * @param <T> pooled object type
 */
public interface Resetter<T> {

  /**
   * Recycles a pooled object state
   *
   * @param object object to recycle
   */
  void recycle(T object);

  /**
   * Resetter for objects that implement {@link Recyclable}
   *
   * @param <T> recyclable object type
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  class ForRecyclable<T extends Recyclable> implements Resetter<T> {
    private static final ForRecyclable INSTANCE = new ForRecyclable();

    public static <T extends Recyclable> Resetter<T> get() {
      return INSTANCE;
    }

    @Override
    public void recycle(Recyclable object) {
      object.resetState();
    }
  }
}
