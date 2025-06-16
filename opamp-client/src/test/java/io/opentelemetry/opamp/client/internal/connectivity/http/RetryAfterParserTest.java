/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.opamp.client.internal.tools.SystemTime;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryAfterParserTest {

  @Test
  void verifyParsing() {
    SystemTime systemTime = mock();
    long currentTimeMillis = 1577836800000L; // Wed, 01 Jan 2020 00:00:00 GMT
    when(systemTime.getCurrentTimeMillis()).thenReturn(currentTimeMillis);

    RetryAfterParser parser = new RetryAfterParser(systemTime);

    assertThat(parser.tryParse("123")).get().isEqualTo(Duration.ofSeconds(123));
    assertThat(parser.tryParse("Wed, 01 Jan 2020 01:00:00 GMT"))
        .get()
        .isEqualTo(Duration.ofHours(1));

    // Check when provided time is older than the current one
    assertThat(parser.tryParse("Tue, 31 Dec 2019 23:00:00 GMT")).isNotPresent();
  }
}
