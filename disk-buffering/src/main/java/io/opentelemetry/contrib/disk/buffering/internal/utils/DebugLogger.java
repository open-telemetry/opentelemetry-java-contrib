/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DebugLogger {
  private final Logger logger;
  private final boolean debugEnabled;

  private DebugLogger(Logger logger, boolean debugEnabled) {
    this.logger = logger;
    this.debugEnabled = debugEnabled;
  }

  public static DebugLogger wrap(Logger logger, boolean debugEnabled) {
    return new DebugLogger(logger, debugEnabled);
  }

  public void log(String msg) {
    log(msg, Level.INFO);
  }

  public void log(String msg, Level level) {
    if (debugEnabled) {
      logger.log(level, msg);
    }
  }

  public void log(String msg, Level level, Throwable e) {
    if (debugEnabled) {
      logger.log(level, msg, e);
    }
  }
}
