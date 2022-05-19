/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.util.List;

public class TestCallbackRegistrar implements CallbackRegistrar, Runnable {

  private final List<Runnable> callbacks;

  public TestCallbackRegistrar(List<Runnable> callbacks) {
    this.callbacks = callbacks;
  }

  @Override
  public CallbackRegistration registerCallback(Runnable callback) {
    callbacks.add(callback);
    return () -> callbacks.remove(callback);
  }

  @Override
  public void close() {
    callbacks.clear();
  }

  @Override
  public void run() {
    for (Runnable callback : callbacks) {
      callback.run();
    }
  }
}
