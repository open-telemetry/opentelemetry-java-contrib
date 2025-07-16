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
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import java.io.IOException;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReadConfigurationEventQueueCollector
    implements Consumer<MetricsCollectorContext> {

  private static final Logger logger =
      LoggerFactory.getLogger(ReadConfigurationEventQueueCollector.class);
  private final long bootTime;
  private final LongGauge maxHandlesGauge;

  public ReadConfigurationEventQueueCollector(Meter meter) {
    this.bootTime = System.currentTimeMillis();
    this.maxHandlesGauge = Metrics.createIbmMqManagerMaxHandles(meter);
  }

  @Nullable
  private PCFMessage findLastUpdate(
      MetricsCollectorContext context, long entryTime, String configurationQueueName)
      throws Exception {
    // find the last update:
    PCFMessage candidate = null;

    boolean consumeEvents =
        context.getQueueManager().getConsumeConfigurationEventInterval() > 0
            && (entryTime - this.bootTime)
                    % context.getQueueManager().getConsumeConfigurationEventInterval()
                == 0;

    MQQueue queue = null;
    try {
      int queueAccessOptions = MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INPUT_SHARED;
      if (!consumeEvents) {
        // we are not consuming the events.
        queueAccessOptions |= MQConstants.MQOO_BROWSE;
      }
      queue = context.getMqQueueManager().accessQueue(configurationQueueName, queueAccessOptions);
      int maxSequenceNumber = 0;
      // keep going until receiving the exception MQConstants.MQRC_NO_MSG_AVAILABLE
      while (true) {
        try {
          MQGetMessageOptions getOptions = new MQGetMessageOptions();
          getOptions.options = MQConstants.MQGMO_NO_WAIT | MQConstants.MQGMO_FAIL_IF_QUIESCING;
          if (!consumeEvents) {
            getOptions.options |= MQConstants.MQGMO_BROWSE_NEXT;
          }
          MQMessage message = new MQMessage();

          queue.get(message, getOptions);
          PCFMessage received = new PCFMessage(message);
          if (received.getMsgSeqNumber() > maxSequenceNumber) {
            maxSequenceNumber = received.getMsgSeqNumber();
            candidate = received;
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
    return candidate;
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    long entryTime = System.currentTimeMillis();
    String configurationQueueName = context.getQueueManager().getConfigurationQueueName();
    logger.info(
        "sending PCF agent request to read configuration events from queue {}",
        configurationQueueName);
    try {

      PCFMessage candidate = findLastUpdate(context, entryTime, configurationQueueName);

      if (candidate == null) {
        if (context.getQueueManager().isRefreshQueueManagerConfigurationEnabled()) {
          // no event found.
          // we issue a refresh request, which will generate a configuration event on the
          // configuration event queue.
          // note this may incur a performance cost to the queue manager.
          PCFMessage request = new PCFMessage(CMQCFC.MQCMD_REFRESH_Q_MGR);
          request.addParameter(CMQCFC.MQIACF_REFRESH_TYPE, CMQCFC.MQRT_CONFIGURATION);
          request.addParameter(CMQCFC.MQIACF_OBJECT_TYPE, CMQC.MQOT_Q_MGR);
          context.send(request);
          // try again:
          candidate = findLastUpdate(context, entryTime, configurationQueueName);
        }
      }

      if (candidate != null) {
        if (context.getMetricsConfig().isIbmMqManagerMaxHandlesEnabled()) {
          int maxHandles = candidate.getIntParameterValue(CMQC.MQIA_MAX_HANDLES);
          maxHandlesGauge.set(
              maxHandles,
              Attributes.of(
                  AttributeKey.stringKey("queue.manager"), context.getQueueManager().getName()));
        }
      }

    } catch (Exception e) {
      logger.error(
          "Unexpected error occurred while collecting configuration events for queue "
              + configurationQueueName,
          e);
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug(
        "Time taken to publish metrics for configuration events is {} milliseconds", exitTime);
  }
}
