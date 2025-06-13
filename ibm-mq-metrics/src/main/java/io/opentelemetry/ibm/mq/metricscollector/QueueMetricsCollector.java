/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.ibm.mq.metricscollector;

import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.api.metrics.Meter;
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
