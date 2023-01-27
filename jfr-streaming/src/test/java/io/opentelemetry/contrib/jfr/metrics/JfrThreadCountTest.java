/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.DAEMON;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_THREADS;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class JfrThreadCountTest {

  @RegisterExtension
  JfrExtension jfrExtension =
      new JfrExtension(
          builder -> builder.disableAllFeatures().enableFeature(JfrFeature.THREAD_METRICS));

  private static final int SAMPLING_INTERVAL = 1000;

  private static void doWork() throws InterruptedException {
    Thread.sleep(2 * SAMPLING_INTERVAL);
  }

  private static boolean isDaemon(LongPointData p) {
    Boolean daemon = p.getAttributes().get(AttributeKey.booleanKey(DAEMON));
    assertThat(daemon).isNotNull();
    return daemon;
  }

  @Test
  void shouldHaveJfrThreadCountEvents() throws Exception {
    // This should generate some events
    Runnable work =
        () -> {
          // create contention between threads for one lock
          try {
            doWork();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        };
    Thread userThread = new Thread(work);
    userThread.setDaemon(false);
    userThread.start();

    Thread daemonThread = new Thread(work);
    daemonThread.setDaemon(true);
    daemonThread.start();

    userThread.join();
    daemonThread.join();

    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.threads.count")
                .hasUnit(UNIT_THREADS)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      assertThat(sumData.getPoints())
                          .map(LongPointData.class::cast)
                          .anyMatch(p -> p.getValue() > 0 && isDaemon(p))
                          .anyMatch(p -> p.getValue() > 0 && !isDaemon(p));
                    }));
  }
}
