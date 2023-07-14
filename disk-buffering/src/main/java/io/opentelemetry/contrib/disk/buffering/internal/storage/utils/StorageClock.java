/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.utils;

import io.opentelemetry.sdk.common.Clock;

/** Internal utility that allows changing the time for testing purposes. */
public final class StorageClock implements Clock {
  private static final StorageClock INSTANCE = new StorageClock();

  public static StorageClock getInstance() {
    return INSTANCE;
  }

  /** Returns the current time in milliseconds. */
  @Override
  public long now() {
    return System.currentTimeMillis();
  }

  @Override
  public long nanoTime() {
    throw new UnsupportedOperationException();
  }
}
