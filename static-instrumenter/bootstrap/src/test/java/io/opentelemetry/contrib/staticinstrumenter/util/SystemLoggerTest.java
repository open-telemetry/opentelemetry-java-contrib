/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class SystemLoggerTest {

  private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

  private static final SystemLogger logger = SystemLogger.getLogger(SystemLoggerTest.class);

  @BeforeEach
  void setUp() {
    System.setErr(new PrintStream(errStreamCaptor));
  }

  @Test
  void format() {
    String message = "Could not open {} because {}";

    String result = logger.format(SystemLogger.LogLevel.INFO, message, "aaa", "bbb");

    assertThat(result)
        .isEqualTo(
            SystemLogger.LogLevel.INFO
                + " "
                + SystemLoggerTest.class.getName()
                + " - Could not open aaa because bbb");
  }

  @Test
  void formatNoArgs() {
    String message = "Some message";

    String result = logger.format(SystemLogger.LogLevel.DEBUG, message);

    assertThat(result)
        .isEqualTo(
            SystemLogger.LogLevel.DEBUG
                + " "
                + SystemLoggerTest.class.getName()
                + " - Some message");
  }

  @Test
  void info() {
    String message = "Could not open {} because {}";

    logger.info(message, "aaa", "bbb");

    assertThat(errStreamCaptor.toString(UTF_8).trim())
        .isEqualTo(
            SystemLogger.LogLevel.INFO
                + " "
                + SystemLoggerTest.class.getName()
                + " - Could not open aaa because bbb");
  }

  @Test
  void error() {
    String message = "Could not open {} because {}";

    logger.error(message, "aaa", "bbb");

    assertThat(errStreamCaptor.toString(UTF_8).trim())
        .isEqualTo(
            SystemLogger.LogLevel.ERROR
                + " "
                + SystemLoggerTest.class.getName()
                + " - Could not open aaa because bbb");
  }

  @Test
  void stackTrace() {
    String message = "Could not open {} because {}";

    Exception e = new IOException("oh no");

    logger.error(message, e, "aaa", "bbb");

    assertThat(errStreamCaptor.toString(UTF_8).trim())
        .startsWith(
            SystemLogger.LogLevel.ERROR
                + " "
                + SystemLoggerTest.class.getName()
                + " - Could not open aaa because bbb")
        .containsIgnoringWhitespaces(Throwables.getStackTrace(e));
  }
}
