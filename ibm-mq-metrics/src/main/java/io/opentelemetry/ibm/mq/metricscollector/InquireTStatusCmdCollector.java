/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InquireTStatusCmdCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(InquireTStatusCmdCollector.class);

  private final LongGauge publishCountGauge;
  private final LongGauge subscriptionCountGauge;

  public InquireTStatusCmdCollector(Meter meter) {
    this.publishCountGauge = Metrics.createIbmMqPublishCount(meter);
    this.subscriptionCountGauge = Metrics.createIbmMqSubscriptionCount(meter);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting metrics for command MQCMD_INQUIRE_TOPIC_STATUS");
    long entryTime = System.currentTimeMillis();

    Set<String> topicGenericNames = context.getTopicIncludeFilterNames();
    //  to query the current status of topics, which is essential for monitoring and managing the
    // publish/subscribe environment in IBM MQ.
    for (String topicGenericName : topicGenericNames) {
      // Request:
      // https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q088140_.htm
      // list of all metrics extracted through MQCMD_INQUIRE_TOPIC_STATUS is mentioned here
      // https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q088150_.htm
      PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS);
      request.addParameter(CMQC.MQCA_TOPIC_STRING, topicGenericName);

      try {
        processPcfRequestAndPublishQMetrics(context, topicGenericName, request);
      } catch (PCFException pcfe) {
        logger.error(
            "PCFException caught while collecting metric for Queue: {} for command MQCMD_INQUIRE_TOPIC_STATUS",
            topicGenericName,
            pcfe);
        PCFMessage[] msgs = (PCFMessage[]) pcfe.exceptionSource;
        for (PCFMessage msg : msgs) {
          logger.error(msg.toString());
        }
        // Don't throw exception as it will stop queue metric colloection
      } catch (Exception mqe) {
        logger.error("MQException caught", mqe);
        // Dont throw exception as it will stop queuemetric colloection
      }
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug(
        "Time taken to publish metrics for all queues is {} milliseconds for command MQCMD_INQUIRE_TOPIC_STATUS",
        exitTime);
  }

  private void processPcfRequestAndPublishQMetrics(
      MetricsCollectorContext context, String topicGenericName, PCFMessage request)
      throws IOException, MQDataException {
    logger.debug(
        "sending PCF agent request to topic metrics for generic topic {} for command MQCMD_INQUIRE_TOPIC_STATUS",
        topicGenericName);
    long startTime = System.currentTimeMillis();
    List<PCFMessage> response = context.send(request);
    long endTime = System.currentTimeMillis() - startTime;
    logger.debug(
        "PCF agent topic metrics query response for generic topic {} for command MQCMD_INQUIRE_TOPIC_STATUS received in {} milliseconds",
        topicGenericName,
        endTime);
    if (response.isEmpty()) {
      logger.debug(
          "Unexpected error while PCFMessage.send() for command MQCMD_INQUIRE_TOPIC_STATUS, response is either null or empty");
      return;
    }

    List<PCFMessage> messages =
        MessageFilter.ofKind("topic")
            .excluding(context.getTopicExcludeFilters())
            .withResourceExtractor(MessageBuddy::topicName)
            .filter(response);

    for (PCFMessage message : messages) {
      String topicName = MessageBuddy.topicName(message);
      logger.debug(
          "Pulling out metrics for topic name {} for command MQCMD_INQUIRE_TOPIC_STATUS",
          topicName);
      extractMetrics(context, message, topicName);
    }
  }

  private void extractMetrics(
      MetricsCollectorContext context, PCFMessage pcfMessage, String topicString)
      throws PCFException {
    Attributes attributes =
        Attributes.of(
            AttributeKey.stringKey("topic.name"),
            topicString,
            AttributeKey.stringKey("queue.manager"),
            context.getQueueManagerName());
    if (context.getMetricsConfig().isIbmMqPublishCountEnabled()) {
      int publisherCount = 0;
      if (pcfMessage.getParameter(CMQC.MQIA_PUB_COUNT) != null) {
        publisherCount = pcfMessage.getIntParameterValue(CMQC.MQIA_PUB_COUNT);
      }
      publishCountGauge.set(publisherCount, attributes);
    }
    if (context.getMetricsConfig().isIbmMqSubscriptionCountEnabled()) {
      int subscriberCount = 0;
      if (pcfMessage.getParameter(CMQC.MQIA_SUB_COUNT) != null) {
        subscriberCount = pcfMessage.getIntParameterValue(CMQC.MQIA_SUB_COUNT);
      }
      subscriptionCountGauge.set(subscriberCount, attributes);
    }
  }
}
