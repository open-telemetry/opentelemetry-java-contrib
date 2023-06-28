/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.utils;

public class TimeProvider {
  private static final TimeProvider INSTANCE = new TimeProvider();

  public static TimeProvider get() {
    return INSTANCE;
  }

  public long getSystemCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
