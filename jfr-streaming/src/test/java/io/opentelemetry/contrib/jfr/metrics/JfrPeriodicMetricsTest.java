/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JfrPeriodicMetricsTest extends AbstractMetricsTest {
  static class Stressor {
    private Stressor() {}

    public static void execute(int numberOfThreads, Runnable task) throws Exception {
      List<Thread> threads = new ArrayList<>();
      for (int n = 0; n < numberOfThreads; ++n) {
        Thread t = new Thread(task);
        threads.add(t);
        t.start();
      }
      for (Thread t : threads) {
        t.join();
      }
    }
  }

  private static final int THREADS = 10;
  private static final int MILLIS = 50;

  static Object monitor = new Object();
  private static int count = 0;

  private static void doWork(Object obj) throws InterruptedException {
    count++;
    synchronized (obj) {
      // Spin until everyone is at critical section. All threads must be running now.
      while (count < THREADS) {
        Thread.sleep(MILLIS);
      }
    }
  }

  @Test
  public void shouldHaveJfrPeriodicEvents() throws Exception {
    // This should generate some events

    Runnable r =
        () -> {
          // create contention between threads for one lock
          try {
            doWork(monitor);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        };
    Stressor.execute(THREADS, r);

    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.cpu.active_threads")
                .hasLongGaugeSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData ->
                                        Assertions.assertTrue(pointData.getValue() >= THREADS)))),
        metric ->
            metric
                .hasName("process.runtime.jvm.cpu.loaded_class_count")
                .hasLongGaugeSatisfying(
                    sum ->
                        sum.hasPointsSatisfying(
                            point ->
                                point.satisfies(
                                    pointData ->
                                        Assertions.assertTrue(pointData.getValue() >= 0)))));
  }
}
