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

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.MQCFIL;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
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
      throw new RuntimeException(e);
    } finally {
      long exitTime = System.currentTimeMillis() - entryTime;
      logger.debug("Time taken to publish metrics for queuemanager is {} milliseconds", exitTime);
    }
  }
}
