/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Collects IBM MQ queue metrics. */
public final class QueueMetricsCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(QueueMetricsCollector.class);

  private final List<Consumer<MetricsCollectorContext>> publishers = new ArrayList<>();
  private final InquireQCmdCollector inquireQueueCmd;

  public QueueMetricsCollector() {
    QueueCollectionBuddy queueBuddy = new QueueCollectionBuddy();
    this.inquireQueueCmd = new InquireQCmdCollector(queueBuddy);
    publishers.add(new InquireQStatusCmdCollector(queueBuddy));
    publishers.add(new ResetQStatsCmdCollector(queueBuddy));
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting queue metrics...");

    // first collect all queue types.
    inquireQueueCmd.accept(context);

    for (Consumer<MetricsCollectorContext> p : publishers) {
      p.accept(context);
    }
  }
}
