/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import io.opentelemetry.ibm.mq.metrics.MetricProducer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopicMetricsCollector implements Consumer<MetricsCollectorContext> {
  private static final Logger logger = LoggerFactory.getLogger(TopicMetricsCollector.class);
  private final InquireTStatusCmdCollector inquireTStatusCmdCollector;

  public TopicMetricsCollector(MetricProducer producer) {
    this.inquireTStatusCmdCollector = new InquireTStatusCmdCollector(producer);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting Topic metrics...");
    inquireTStatusCmdCollector.accept(context);
  }
}
