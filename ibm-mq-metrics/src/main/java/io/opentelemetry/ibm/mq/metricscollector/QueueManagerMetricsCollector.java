/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static io.opentelemetry.ibm.mq.metrics.IbmMqAttributes.IBM_MQ_QUEUE_MANAGER;
import static io.opentelemetry.ibm.mq.metrics.Metrics.MIBY_TO_BYTES;

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.ibm.mq.metrics.MetricProducer;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is responsible for queue manager metric collection. */
public final class QueueManagerMetricsCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(QueueManagerMetricsCollector.class);

  private final MetricProducer producer;

  public QueueManagerMetricsCollector(MetricProducer producer) {
    this.producer = producer;
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
      Attributes attributes = Attributes.of(IBM_MQ_QUEUE_MANAGER, context.getQueueManagerName());
      if (context.getMetricsConfig().isIbmMqManagerStatusEnabled()) {
        int status = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_Q_MGR_STATUS);
        producer.recordIbmMqManagerStatus(status, attributes);
      }
      if (context.getMetricsConfig().isIbmMqConnectionCountEnabled()) {
        int count = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_CONNECTION_COUNT);
        producer.recordIbmMqConnectionCount(count, attributes);
      }
      if (context.getMetricsConfig().isIbmMqRestartLogSizeEnabled()) {
        int logSize = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_RESTART_LOG_SIZE);
        producer.recordIbmMqRestartLogSize(MIBY_TO_BYTES.apply(logSize), attributes);
      }
      if (context.getMetricsConfig().isIbmMqReusableLogSizeEnabled()) {
        int logSize = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_REUSABLE_LOG_SIZE);
        producer.recordIbmMqReusableLogSize(MIBY_TO_BYTES.apply(logSize), attributes);
      }
      if (context.getMetricsConfig().isIbmMqArchiveLogSizeEnabled()) {
        int logSize = responses.get(0).getIntParameterValue(CMQCFC.MQIACF_ARCHIVE_LOG_SIZE);
        producer.recordIbmMqArchiveLogSize(MIBY_TO_BYTES.apply(logSize), attributes);
      }
      if (context.getMetricsConfig().isIbmMqManagerMaxActiveChannelsEnabled()) {
        int maxActiveChannels = context.getQueueManager().getMaxActiveChannels();
        producer.recordIbmMqManagerMaxActiveChannels(maxActiveChannels, attributes);
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
