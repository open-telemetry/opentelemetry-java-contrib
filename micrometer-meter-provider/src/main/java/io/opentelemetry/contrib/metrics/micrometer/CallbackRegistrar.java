/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

/** Registers callbacks to be invoked when asynchronous instruments are polled. */
public interface CallbackRegistrar extends AutoCloseable {
  CallbackRegistration registerCallback(Runnable runnable);

  @Override
  void close();
}
