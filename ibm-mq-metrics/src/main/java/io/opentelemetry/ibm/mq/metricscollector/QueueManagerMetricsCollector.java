/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is responsible for queue manager metric collection. */
public final class QueueManagerMetricsCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(QueueManagerMetricsCollector.class);

  private final LongGauge statusGauge;
  private final LongGauge connectionCountGauge;
  private final LongGauge restartLogSizeGauge;
  private final LongGauge reuseLogSizeGauge;
  private final LongGauge archiveLogSizeGauge;
  private final LongGauge maxActiveChannelsGauge;

  public QueueManagerMetricsCollector(Meter meter) {
    this.statusGauge = Metrics.createMqManagerStatus(meter);
    this.connectionCountGauge = Metrics.createMqConnectionCount(meter);
    this.restartLogSizeGauge = Metrics.createMqRestartLogSize(meter);
    this.reuseLogSizeGauge = Metrics.createMqReusableLogSize(meter);
    this.archiveLogSizeGauge = Metrics.createMqArchiveLogSize(meter);
    this.maxActiveChannelsGauge = Metrics.createMqManagerMaxActiveChannels(meter);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    long entryTime = System.currentTimeMillis();
    logger.debug(
        "publishMetrics entry time for queuemanager {} is {} milliseconds",
        context.getQueueManagerName(),
        entryTime);
    // CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS is 161
    PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
    // CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS is 1229
    request.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS, new int[] {CMQCFC.MQIACF_ALL});
    try {
      // Note that agent.send() method is synchronized
      logger.debug(
          "sending PCF agent request to query queuemanager {}", context.getQueueManagerName());
      long startTime = System.currentTimeMillis();
      List<PCFMessage> responses = context.send(request);
      long endTime = System.currentTimeMillis() - startTime;
      logger.debug(
          "PCF agent queuemanager metrics query response for {} received in {} milliseconds",
          context.getQueueManagerName(),
          endTime);
      if (responses.isEmpty()) {
        logger.debug("Unexpected error while PCFMessage.send(), response is empty");
        return;
      }
      if (context.getMetricsConfig().isMqManagerStatusEnabled()) {
        int status = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_Q_MGR_STATUS);
        statusGauge.set(
            status,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
      if (context.getMetricsConfig().isMqConnectionCountEnabled()) {
        int count = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_CONNECTION_COUNT);
        connectionCountGauge.set(
            count,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
      if (context.getMetricsConfig().isMqRestartLogSizeEnabled()) {
        int logSize = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_RESTART_LOG_SIZE);
        restartLogSizeGauge.set(
            logSize,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
      if (context.getMetricsConfig().isMqReusableLogSizeEnabled()) {
        int logSize = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_REUSABLE_LOG_SIZE);
        reuseLogSizeGauge.set(
            logSize,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
      if (context.getMetricsConfig().isMqArchiveLogSizeEnabled()) {
        int logSize = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_ARCHIVE_LOG_SIZE);
        archiveLogSizeGauge.set(
            logSize,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
      if (context.getMetricsConfig().isMqManagerMaxActiveChannelsEnabled()) {
        int maxActiveChannels = context.getQueueManager().getMaxActiveChannels();
        maxActiveChannelsGauge.set(
            maxActiveChannels,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new IllegalStateException(e);
    } finally {
      long exitTime = System.currentTimeMillis() - entryTime;
      logger.debug("Time taken to publish metrics for queuemanager is {} milliseconds", exitTime);
    }
  }
}
