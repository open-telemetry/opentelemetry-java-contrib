/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import java.util.ArrayList;

public class TestCallbackRegistrar extends ArrayList<Runnable>
    implements CallbackRegistrar, Runnable {
  @Override
  public CallbackRegistration registerCallback(Runnable callback) {
    this.add(callback);
    return () -> this.remove(callback);
  }

  @Override
  public void close() {
    this.clear();
  }

  @Override
  public void run() {
    for (Runnable callback : this) {
      callback.run();
    }
  }
}
