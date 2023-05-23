/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

public interface CallbackRegistrar extends AutoCloseable {
  CallbackRegistration registerCallback(Runnable runnable);

  @Override
  void close();
}
