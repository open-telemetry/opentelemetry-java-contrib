/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.utils;

public class TimeProvider {
  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // Needed for tests
  private static TimeProvider instance = new TimeProvider();

  public static TimeProvider get() {
    return instance;
  }

  public long getSystemCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
