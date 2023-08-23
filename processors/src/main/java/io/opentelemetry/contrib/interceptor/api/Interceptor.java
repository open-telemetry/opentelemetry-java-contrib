package io.opentelemetry.contrib.interceptor.api;

/**
 * Intercepts a signal before it gets exported. The signal can get updated and/or filtered out based
 * on each interceptor implementation.
 */
public interface Interceptor<T> {

  /**
   * Intercepts a signal.
   *
   * @param item The signal object.
   * @return The received signal modified (or null for excluding this signal from getting exported).
   *     If there's no operation needed to be done for a specific signal, it should be returned as
   *     is.
   */
  T intercept(T item);
}
