package io.opentelemetry.opamp.client.internal.state;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
abstract class InMemoryState<T> implements State<T> {
  private final AtomicReference<T> state = new AtomicReference<>();

  public InMemoryState(@Nonnull T initialValue) {
    state.set(initialValue);
  }

  public void set(@Nonnull T value) {
    state.set(value);
  }

  @Nonnull
  @Override
  public T get() {
    return Objects.requireNonNull(state.get());
  }
}
