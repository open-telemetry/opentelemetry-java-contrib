/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.tools;

/** Utility to be able to mock the current system time for testing purposes. */
public class SystemTime {
  private static final SystemTime INSTANCE = new SystemTime();

  public static SystemTime getInstance() {
    return INSTANCE;
  }

  private SystemTime() {}

  public long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
