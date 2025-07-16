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
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
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

// Reads queue manager events and counts them as metrics
public final class QueueManagerEventCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(QueueManagerEventCollector.class);
  private final LongCounter authorityEventCounter;

  public QueueManagerEventCollector(Meter meter) {
    this.authorityEventCounter = Metrics.createIbmMqUnauthorizedEvent(meter);
  }

  private void readEvents(MetricsCollectorContext context, String queueManagerEventsQueueName)
      throws Exception {

    MQQueue queue = null;
    try {
      int queueAccessOptions = MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INPUT_SHARED;
      queue =
          context.getMqQueueManager().accessQueue(queueManagerEventsQueueName, queueAccessOptions);
      // keep going until receiving the exception MQConstants.MQRC_NO_MSG_AVAILABLE
      while (true) {
        try {
          MQGetMessageOptions getOptions = new MQGetMessageOptions();
          getOptions.options = MQConstants.MQGMO_NO_WAIT | MQConstants.MQGMO_FAIL_IF_QUIESCING;
          MQMessage message = new MQMessage();

          queue.get(message, getOptions);
          PCFMessage received = new PCFMessage(message);
          if (received.getReason() == CMQC.MQRC_NOT_AUTHORIZED) {

            if (context.getMetricsConfig().isIbmMqUnauthorizedEventEnabled()) {
              String username = received.getStringParameterValue(CMQCFC.MQCACF_USER_IDENTIFIER);
              String applicationName = received.getStringParameterValue(CMQCFC.MQCACF_APPL_NAME);
              authorityEventCounter.add(
                  1,
                  Attributes.of(
                      AttributeKey.stringKey("queue.manager"),
                      context.getQueueManagerName(),
                      AttributeKey.stringKey("user.name"),
                      username,
                      AttributeKey.stringKey("service.name"),
                      applicationName));
            }
          } else {
            logger.debug("Unknown event reason {}", received.getReason());
          }

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
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    long entryTime = System.currentTimeMillis();
    String queueManagerEventsQueueName = context.getQueueManager().getQueueManagerEventsQueueName();
    logger.info(
        "sending PCF agent request to read queue manager events from queue {}",
        queueManagerEventsQueueName);
    try {
      readEvents(context, queueManagerEventsQueueName);
    } catch (Exception e) {
      logger.error(
          "Unexpected error occurred while collecting queue manager events for queue "
              + queueManagerEventsQueueName,
          e);
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug(
        "Time taken to publish metrics for queue manager events is {} milliseconds", exitTime);
  }
}
