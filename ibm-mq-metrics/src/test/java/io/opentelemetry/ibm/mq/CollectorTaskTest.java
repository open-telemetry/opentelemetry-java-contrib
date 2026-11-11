/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CollectorTaskTest {

  @Test
  void cancelledBeforeStartNeverRunsAndCountsDownOnce() {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger runs = new AtomicInteger();
    CollectorTask task = new CollectorTask(runs::incrementAndGet, latch);

    task.cancelIfNotStarted();
    task.call();
    task.cancelIfNotStarted();

    assertThat(runs).hasValue(0);
    assertThat(latch.getCount()).isZero();
  }

  @Test
  void cancelWhileRunningDoesNotCountDownUntilCollectorExits() {
    CountDownLatch latch = new CountDownLatch(1);
    CollectorTask[] holder = new CollectorTask[1];
    long[] countDuringRun = new long[1];
    // cancellation lands after the task entered its body but before it finished
    holder[0] =
        new CollectorTask(
            () -> {
              holder[0].cancelIfNotStarted();
              countDuringRun[0] = latch.getCount();
            },
            latch);

    holder[0].call();

    assertThat(countDuringRun[0]).isEqualTo(1);
    assertThat(latch.getCount()).isZero();
  }

  @Test
  void countsDownWhenCollectorThrows() {
    CountDownLatch latch = new CountDownLatch(1);
    CollectorTask task =
        new CollectorTask(
            () -> {
              throw new IllegalStateException("boom");
            },
            latch);

    try {
      task.call();
    } catch (IllegalStateException expected) {
      // ignored
    }

    assertThat(latch.getCount()).isZero();
  }
}
