/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueueMetricsCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(QueueMetricsCollector.class);

  private final List<Consumer<MetricsCollectorContext>> publishers = new ArrayList<>();
  private final InquireQCmdCollector inquireQueueCmd;
  private final ExecutorService threadPool;
  private final ConfigWrapper config;

  public QueueMetricsCollector(Meter meter, ExecutorService threadPool, ConfigWrapper config) {
    this.threadPool = threadPool;
    this.config = config;
    QueueCollectionBuddy queueBuddy =
        new QueueCollectionBuddy(meter, new QueueCollectorSharedState());
    this.inquireQueueCmd = new InquireQCmdCollector(queueBuddy);
    publishers.add(new InquireQStatusCmdCollector(queueBuddy));
    publishers.add(new ResetQStatsCmdCollector(queueBuddy));
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting queue metrics...");

    // first collect all queue types.
    inquireQueueCmd.accept(context);

    // schedule all other jobs in parallel.
    List<Callable<Void>> taskJobs = new ArrayList<>();
    for (Consumer<MetricsCollectorContext> p : publishers) {
      taskJobs.add(
          () -> {
            p.accept(context);
            return null;
          });
    }

    try {
      int timeout = this.config.getInt("queueMetricsCollectionTimeoutInSeconds", 20);
      threadPool.invokeAll(taskJobs, timeout, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.error("The thread was interrupted ", e);
    }
  }
}
