/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import io.opentelemetry.opamp.client.internal.tools.SystemTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RetryAfterParser {
  private final SystemTime systemTime;
  public static final Pattern SECONDS_PATTERN = Pattern.compile("\\d+");
  public static final Pattern DATE_PATTERN =
      Pattern.compile(
          "^([A-Za-z]{3}, [0-3][0-9] [A-Za-z]{3} [0-9]{4} [0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT)$");
  private static final SimpleDateFormat dateFormat =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

  public static RetryAfterParser getInstance() {
    return new RetryAfterParser(SystemTime.getInstance());
  }

  RetryAfterParser(SystemTime systemTime) {
    this.systemTime = systemTime;
  }

  public Duration parse(String value) {
    if (SECONDS_PATTERN.matcher(value).matches()) {
      return Duration.ofSeconds(Long.parseLong(value));
    } else if (DATE_PATTERN.matcher(value).matches()) {
      return Duration.ofMillis(toMilliseconds(value) - systemTime.getCurrentTimeMillis());
    }
    throw new IllegalArgumentException("Invalid Retry-After value: " + value);
  }

  @SuppressWarnings({"JavaUtilDate", "ThrowSpecificExceptions"})
  private static long toMilliseconds(String value) {
    try {
      return dateFormat.parse(value).getTime();
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
