/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util;

import java.text.MessageFormat;
import java.util.regex.Pattern;

/**
 * A custom logger that uses System.err. This is necessary because we shouldn't configure any
 * conventional logger in {@code
 * io.opentelemetry.contrib.staticinstrumenter.agent.OpenTelemetryStaticAgent} (see {@code
 * io.opentelemetry.javaagent.OpenTelemetryAgent} docs).
 */
@SuppressWarnings("SystemOut")
public class SystemLogger {

  private static final String DEBUG_SYSTEM_LOGGER = "otel.javaagent.static-instrumentation.debug";

  private final boolean debugEnabled;

  enum LogLevel {
    INFO,
    DEBUG,
    ERROR
  }

  private final Class<?> clazz;

  public static SystemLogger getLogger(Class<?> clazz) {
    return new SystemLogger(clazz);
  }

  private SystemLogger(Class<?> clazz) {
    this.debugEnabled = Boolean.getBoolean(DEBUG_SYSTEM_LOGGER);
    this.clazz = clazz;
  }

  public void info(String message, Object... args) {
    System.err.println(format(LogLevel.INFO, message, args));
  }

  public void debug(String message, Object... args) {
    if (debugEnabled) {
      System.err.println(format(LogLevel.DEBUG, message, args));
    }
  }

  public void error(String message, Object... args) {
    System.err.println(format(LogLevel.ERROR, message, args));
  }

  public void error(String message, Throwable t, Object... args) {
    System.err.println(format(LogLevel.ERROR, message, args));
    t.printStackTrace();
  }

  // visible for testing
  String format(LogLevel level, String message, Object... args) {
    int i = 2;
    while (message.contains("{}")) {
      message = message.replaceFirst(Pattern.quote("{}"), "{" + i++ + "}");
    }
    Object[] newArgs = new Object[args.length + 2];
    newArgs[0] = level;
    newArgs[1] = clazz.getName();

    System.arraycopy(args, 0, newArgs, 2, args.length);

    return MessageFormat.format("{0} {1} - " + message, newArgs);
  }
}
