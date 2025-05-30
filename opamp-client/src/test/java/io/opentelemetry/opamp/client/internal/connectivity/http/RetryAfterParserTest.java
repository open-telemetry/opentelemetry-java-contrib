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
    long currentTimeMillis = 1577836800000L;
    when(systemTime.getCurrentTimeMillis()).thenReturn(currentTimeMillis);

    RetryAfterParser parser = new RetryAfterParser(systemTime);

    assertThat(parser.parse("123")).isEqualTo(Duration.ofSeconds(123));
    assertThat(parser.parse("Wed, 01 Jan 2020 01:00:00 GMT")).isEqualTo(Duration.ofHours(1));
  }
}
