/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.MQCFIL;
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

/** This class is responsible for queue metric collection. */
public final class InquireQueueManagerCmdCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger =
      LoggerFactory.getLogger(InquireQueueManagerCmdCollector.class);
  private final LongGauge statisticsIntervalGauge;

  public InquireQueueManagerCmdCollector(Meter meter) {
    this.statisticsIntervalGauge = Metrics.createMqManagerStatisticsInterval(meter);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    long entryTime = System.currentTimeMillis();
    logger.debug(
        "publishMetrics entry time for queuemanager {} is {} milliseconds",
        context.getQueueManagerName(),
        entryTime);
    // CMQCFC.MQCMD_INQUIRE_Q_MGR is 2
    PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR);
    // request.addParameter(CMQC.MQCA_Q_MGR_NAME, "*");
    // CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS is 1001
    request.addParameter(
        new MQCFIL(MQConstants.MQIACF_Q_MGR_ATTRS, new int[] {MQConstants.MQIACF_ALL}));
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
        logger.debug("Unexpected error while PCFMessage.send(), response is either null or empty");
        return;
      }
      if (context.getMetricsConfig().isMqManagerStatisticsIntervalEnabled()) {
        int interval = responses.get(0).getIntParameterValue(CMQC.MQIA_STATISTICS_INTERVAL);
        statisticsIntervalGauge.set(
            interval,
            Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));
      }
    } catch (Exception e) {
      logger.error("Error collecting QueueManagerCmd metrics", e);
      throw new IllegalStateException(e);
    } finally {
      long exitTime = System.currentTimeMillis() - entryTime;
      logger.debug("Time taken to publish metrics for queuemanager is {} milliseconds", exitTime);
    }
  }
}
