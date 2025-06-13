/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import io.opentelemetry.api.metrics.Meter;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopicMetricsCollector implements Consumer<MetricsCollectorContext> {
  private static final Logger logger = LoggerFactory.getLogger(TopicMetricsCollector.class);
  private final InquireTStatusCmdCollector inquireTStatusCmdCollector;

  public TopicMetricsCollector(Meter meter) {
    this.inquireTStatusCmdCollector = new InquireTStatusCmdCollector(meter);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting Topic metrics...");
    inquireTStatusCmdCollector.accept(context);
  }
}
