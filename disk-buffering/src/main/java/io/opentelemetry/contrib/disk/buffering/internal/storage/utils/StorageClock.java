/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.utils;

import io.opentelemetry.sdk.common.Clock;
import java.util.concurrent.TimeUnit;

public class StorageClock implements Clock {
  private static final StorageClock INSTANCE = new StorageClock();

  public static StorageClock getInstance() {
    return INSTANCE;
  }

  @Override
  public long now() {
    return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
  }

  @Override
  public long nanoTime() {
    return System.nanoTime();
  }
}
