/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExponentialBackoffPeriodicDelayTest {
  @Test
  void verifyDelayUpdates() {
    ExponentialBackoffPeriodicDelay delay =
        new ExponentialBackoffPeriodicDelay(Duration.ofSeconds(1));

    assertThat(delay.getNextDelay()).isEqualTo(Duration.ofSeconds(1));
    assertThat(delay.getNextDelay()).isEqualTo(Duration.ofSeconds(2));
    assertThat(delay.getNextDelay()).isEqualTo(Duration.ofSeconds(4));
    assertThat(delay.getNextDelay()).isEqualTo(Duration.ofSeconds(8));
    assertThat(delay.getNextDelay()).isEqualTo(Duration.ofSeconds(16));

    // Reset
    delay.reset();
    assertThat(delay.getNextDelay()).isEqualTo(Duration.ofSeconds(1));
  }
}
