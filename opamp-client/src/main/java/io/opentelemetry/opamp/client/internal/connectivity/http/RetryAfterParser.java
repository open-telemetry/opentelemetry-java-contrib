/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import io.opentelemetry.opamp.client.internal.tools.SystemTime;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class RetryAfterParser {
  private final SystemTime systemTime;
  private static final Pattern SECONDS_PATTERN = Pattern.compile("\\d+");
  private static final Pattern DATE_PATTERN =
      Pattern.compile(
          "[A-Za-z]{3}, [0-3][0-9] [A-Za-z]{3} [0-9]{4} [0-2][0-9]:[0-5][0-9]:[0-5][0-9] GMT");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

  public static RetryAfterParser getInstance() {
    return new RetryAfterParser(SystemTime.getInstance());
  }

  RetryAfterParser(SystemTime systemTime) {
    this.systemTime = systemTime;
  }

  public Optional<Duration> tryParse(String value) {
    Duration duration = null;
    if (SECONDS_PATTERN.matcher(value).matches()) {
      duration = Duration.ofSeconds(Long.parseLong(value));
    } else if (DATE_PATTERN.matcher(value).matches()) {
      long difference = toMilliseconds(value) - systemTime.getCurrentTimeMillis();
      if (difference > 0) {
        duration = Duration.ofMillis(difference);
      }
    }
    return Optional.ofNullable(duration);
  }

  private static long toMilliseconds(String value) {
    return ZonedDateTime.parse(value, DATE_FORMAT).toInstant().toEpochMilli();
  }
}
