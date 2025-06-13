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

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.io.IOException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Captures metrics from events logged to the queue manager performance event queue.
public final class PerformanceEventQueueCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger =
      LoggerFactory.getLogger(PerformanceEventQueueCollector.class);
  private final LongCounter fullQueueDepthCounter;
  private final LongCounter highQueueDepthCounter;
  private final LongCounter lowQueueDepthCounter;

  public PerformanceEventQueueCollector(Meter meter) {
    this.fullQueueDepthCounter = Metrics.createMqQueueDepthFullEvent(meter);
    this.highQueueDepthCounter = Metrics.createMqQueueDepthHighEvent(meter);
    this.lowQueueDepthCounter = Metrics.createMqQueueDepthLowEvent(meter);
  }

  private void readEvents(MetricsCollectorContext context, String performanceEventsQueueName)
      throws Exception {

    MQQueue queue = null;
    int counter = 0;
    try {
      int queueAccessOptions = MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INPUT_SHARED;
      queue =
          context.getMqQueueManager().accessQueue(performanceEventsQueueName, queueAccessOptions);
      // keep going until receiving the exception MQConstants.MQRC_NO_MSG_AVAILABLE
      logger.debug("Start reading events from performance queue {}", performanceEventsQueueName);
      while (true) {
        try {
          MQGetMessageOptions getOptions = new MQGetMessageOptions();
          getOptions.options = MQConstants.MQGMO_NO_WAIT | MQConstants.MQGMO_FAIL_IF_QUIESCING;
          MQMessage message = new MQMessage();

          queue.get(message, getOptions);
          PCFMessage receivedMsg = new PCFMessage(message);
          incrementCounterByEventType(context, receivedMsg);
          counter++;
        } catch (MQException e) {
          if (e.reasonCode != MQConstants.MQRC_NO_MSG_AVAILABLE) {
            logger.error(e.getMessage(), e);
          }
          break;
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          break;
        }
      }
    } finally {
      if (queue != null) {
        queue.close();
      }
    }
    logger.debug("Read {} events from performance queue {}", counter, performanceEventsQueueName);
  }

  private void incrementCounterByEventType(MetricsCollectorContext context, PCFMessage receivedMsg)
      throws PCFException {
    String queueName = receivedMsg.getStringParameterValue(CMQC.MQCA_BASE_OBJECT_NAME).trim();
    Attributes attributes =
        Attributes.of(
            AttributeKey.stringKey("queue.manager"),
            context.getQueueManagerName(),
            AttributeKey.stringKey("queue.name"),
            queueName);
    switch (receivedMsg.getReason()) {
      case CMQC.MQRC_Q_FULL:
        if (context.getMetricsConfig().isMqQueueDepthFullEventEnabled()) {
          fullQueueDepthCounter.add(1, attributes);
        }
        break;
      case CMQC.MQRC_Q_DEPTH_HIGH:
        if (context.getMetricsConfig().isMqQueueDepthHighEventEnabled()) {
          highQueueDepthCounter.add(1, attributes);
        }
        break;
      case CMQC.MQRC_Q_DEPTH_LOW:
        if (context.getMetricsConfig().isMqQueueDepthLowEventEnabled()) {
          lowQueueDepthCounter.add(1, attributes);
        }
        break;
      default:
        logger.debug("Unknown event reason {}", receivedMsg.getReason());
    }
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    long entryTime = System.currentTimeMillis();
    String performanceEventsQueueName = context.getQueueManager().getPerformanceEventsQueueName();
    logger.info(
        "sending PCF agent request to read performance events from queue {}",
        performanceEventsQueueName);
    try {
      readEvents(context, performanceEventsQueueName);
    } catch (Exception e) {
      logger.error(
          "Unexpected error occurred while collecting performance events for queue "
              + performanceEventsQueueName,
          e);
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug(
        "Time taken to publish metrics for performance events is {} milliseconds", exitTime);
  }
}
