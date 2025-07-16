/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.metrics.Metrics;
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
    this.fullQueueDepthCounter = Metrics.createIbmMqQueueDepthFullEvent(meter);
    this.highQueueDepthCounter = Metrics.createIbmMqQueueDepthHighEvent(meter);
    this.lowQueueDepthCounter = Metrics.createIbmMqQueueDepthLowEvent(meter);
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
        if (context.getMetricsConfig().isIbmMqQueueDepthFullEventEnabled()) {
          fullQueueDepthCounter.add(1, attributes);
        }
        break;
      case CMQC.MQRC_Q_DEPTH_HIGH:
        if (context.getMetricsConfig().isIbmMqQueueDepthHighEventEnabled()) {
          highQueueDepthCounter.add(1, attributes);
        }
        break;
      case CMQC.MQRC_Q_DEPTH_LOW:
        if (context.getMetricsConfig().isIbmMqQueueDepthLowEventEnabled()) {
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
