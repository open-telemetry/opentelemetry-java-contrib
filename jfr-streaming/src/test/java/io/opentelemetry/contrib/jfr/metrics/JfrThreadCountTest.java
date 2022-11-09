/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.UNIT_THREADS;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JfrThreadCountTest extends AbstractMetricsTest {
  private static final int SAMPLING_INTERVAL = 1000;

  private static void doWork() throws InterruptedException {
    Thread.sleep(2 * SAMPLING_INTERVAL);
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

    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.threads.count")
                .hasUnit(UNIT_THREADS)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      boolean foundDaemon = false;
                      boolean foundNonDaemon = false;
                      for (PointData point : sumData.getPoints()) {
                        LongPointData longPoint = (LongPointData) point;
                        if (longPoint.getValue() > 0
                            && longPoint
                                .getAttributes()
                                .asMap()
                                .get(AttributeKey.stringKey("daemon"))
                                .equals("false")) {
                          foundNonDaemon = true;
                        } else if (longPoint.getValue() > 0
                            && longPoint
                                .getAttributes()
                                .asMap()
                                .get(AttributeKey.stringKey("daemon"))
                                .equals("true")) {
                          foundDaemon = true;
                        }
                      }
                      Assertions.assertTrue(foundDaemon && foundNonDaemon);
                    }));
  }
}
