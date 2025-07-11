package io.opentelemetry.opamp.client.internal.state;

import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * <p>Implementations of state that cannot be stored in memory. Users would need to implement its
 * {@link #get()} method and ensure to call {@link #notifyUpdate()} whenever new data is available
 * for the next client request.
 */
public abstract class ObservableState<T> implements State<T> {
  private final Set<Listener> listeners = Collections.synchronizedSet(new HashSet<>());

  public final void addListener(Listener listener) {
    listeners.add(listener);
  }

  public final void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public final void notifyUpdate() {
    synchronized (listeners) {
      for (Listener listener : listeners) {
        listener.onStateUpdate(getFieldType());
      }
    }
  }

  public interface Listener {
    /**
     * Notifies that there's new data available for this state, so that the client includes it in
     * the next request.
     *
     * @param type The field type associated to this state.
     */
    void onStateUpdate(Field type);
  }
}
