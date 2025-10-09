/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.delay;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPeriodicDelayTest {
  @Test
  public void verifyDelayBehavior() {
    RetryPeriodicDelay retryPeriodicDelay = RetryPeriodicDelay.create(Duration.ofSeconds(1));

    // Without suggested delay
    assertThat(retryPeriodicDelay.getNextDelay()).isEqualTo(Duration.ofSeconds(1));
    assertThat(retryPeriodicDelay.getNextDelay()).isEqualTo(Duration.ofSeconds(2));
    assertThat(retryPeriodicDelay.getNextDelay()).isEqualTo(Duration.ofSeconds(4));
    retryPeriodicDelay.reset();
    assertThat(retryPeriodicDelay.getNextDelay()).isEqualTo(Duration.ofSeconds(1));

    // With suggested delay
    retryPeriodicDelay.suggestDelay(Duration.ofSeconds(5));
    assertThat(retryPeriodicDelay.getNextDelay()).isEqualTo(Duration.ofSeconds(5));
    retryPeriodicDelay.reset();
    assertThat(retryPeriodicDelay.getNextDelay()).isEqualTo(Duration.ofSeconds(1));
  }
}
