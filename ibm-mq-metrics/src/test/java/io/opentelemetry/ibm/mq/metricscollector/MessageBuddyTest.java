/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class MessageBuddyTest {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH.mm.ss", Locale.ROOT);

  @Test
  void queueManagerUptimeUsesUtcAndDoesNotDriftWithSystemTimezone() throws PCFException {
    TimeZone originalTz = TimeZone.getDefault();
    try {
      Instant startInstant = Instant.now().minusSeconds(600);
      LocalDateTime startUtc = LocalDateTime.ofInstant(startInstant, ZoneOffset.UTC);
      String startDate = startUtc.format(DATE_FORMATTER);
      String startTime = startUtc.format(TIME_FORMATTER);

      PCFMessage message = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS, 1, true);
      message.addParameter(CMQCFC.MQCACF_Q_MGR_START_DATE, startDate);
      message.addParameter(CMQCFC.MQCACF_Q_MGR_START_TIME, startTime);

      TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
      long beforeFirst = Instant.now().getEpochSecond();
      long firstResult = MessageBuddy.queueManagerUptime(message);
      long afterFirst = Instant.now().getEpochSecond();

      TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
      long beforeSecond = Instant.now().getEpochSecond();
      long secondResult = MessageBuddy.queueManagerUptime(message);
      long afterSecond = Instant.now().getEpochSecond();

      long expectedLowerFirst = beforeFirst - startInstant.getEpochSecond();
      long expectedUpperFirst = afterFirst - startInstant.getEpochSecond();
      assertThat(firstResult).isBetween(expectedLowerFirst, expectedUpperFirst);

      long expectedLowerSecond = beforeSecond - startInstant.getEpochSecond();
      long expectedUpperSecond = afterSecond - startInstant.getEpochSecond();
      assertThat(secondResult).isBetween(expectedLowerSecond, expectedUpperSecond);

      // Changing the JVM default timezone should not materially change uptime when UTC is used.
      assertThat(Math.abs(firstResult - secondResult)).isLessThanOrEqualTo(2);
    } finally {
      TimeZone.setDefault(originalTz);
    }
  }
}
