/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.state;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
abstract class InMemoryState<T> implements State<T> {
  private final AtomicReference<T> state = new AtomicReference<>();

  public InMemoryState(@Nullable T initialValue) {
    state.set(initialValue);
  }

  /**
   * Set a new state's value in a thread safe way.
   *
   * @param value a new value stored in the state
   * @return <code>true</code> if new value is different that the previous one, <code>false</code>
   *     otherwise.
   */
  public boolean set(T value) {
    if (value == null) {
      throw new IllegalArgumentException("The value must not be null");
    }
    T previousValue = state.getAndSet(value);
    return !Objects.equals(previousValue, value);
  }

  @Nullable
  @Override
  public T get() {
    return state.get();
  }
}
