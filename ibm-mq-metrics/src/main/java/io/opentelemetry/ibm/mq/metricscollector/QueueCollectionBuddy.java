/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static com.ibm.mq.constants.CMQC.MQQT_ALIAS;
import static com.ibm.mq.constants.CMQC.MQQT_CLUSTER;
import static com.ibm.mq.constants.CMQC.MQQT_LOCAL;
import static com.ibm.mq.constants.CMQC.MQQT_MODEL;
import static com.ibm.mq.constants.CMQC.MQQT_REMOTE;

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.MQCFIL;
import com.ibm.mq.headers.pcf.MQCFIN;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFParameter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collaborator buddy of the queue collectors that helps them to send a message, process the
 * response, and generate metrics.
 */
final class QueueCollectionBuddy {
  private static final Logger logger = LoggerFactory.getLogger(QueueCollectionBuddy.class);
  private final Map<Integer, AllowedGauge> gauges = new HashMap<>();

  private final QueueCollectorSharedState sharedState;
  private final LongGauge onqtimeShort;
  private final LongGauge onqtimeLong;

  @FunctionalInterface
  private interface AllowedGauge {
    void set(MetricsCollectorContext context, Integer value, Attributes attributes);
  }

  private static AllowedGauge createAllowedGauge(
      LongGauge gauge, Function<MetricsConfig, Boolean> allowed) {
    return (context, val, attributes) -> {
      if (allowed.apply(context.getMetricsConfig())) {
        gauge.set(val, attributes);
      }
    };
  }

  QueueCollectionBuddy(Meter meter, QueueCollectorSharedState sharedState) {
    this.sharedState = sharedState;
    gauges.put(
        CMQC.MQIA_CURRENT_Q_DEPTH,
        createAllowedGauge(
            Metrics.createMqQueueDepth(meter), MetricsConfig::isMqQueueDepthEnabled));
    gauges.put(
        CMQC.MQIA_MAX_Q_DEPTH,
        createAllowedGauge(
            Metrics.createMqMaxQueueDepth(meter), MetricsConfig::isMqMaxQueueDepthEnabled));
    gauges.put(
        CMQC.MQIA_OPEN_INPUT_COUNT,
        createAllowedGauge(
            Metrics.createMqOpenInputCount(meter), MetricsConfig::isMqOpenInputCountEnabled));
    gauges.put(
        CMQC.MQIA_OPEN_OUTPUT_COUNT,
        createAllowedGauge(
            Metrics.createMqOpenOutputCount(meter), MetricsConfig::isMqOpenOutputCountEnabled));
    gauges.put(
        CMQC.MQIA_Q_SERVICE_INTERVAL,
        createAllowedGauge(
            Metrics.createMqServiceInterval(meter), MetricsConfig::isMqServiceIntervalEnabled));
    gauges.put(
        CMQC.MQIA_Q_SERVICE_INTERVAL_EVENT,
        createAllowedGauge(
            Metrics.createMqServiceIntervalEvent(meter),
            MetricsConfig::isMqServiceIntervalEventEnabled));
    gauges.put(
        CMQCFC.MQIACF_OLDEST_MSG_AGE,
        createAllowedGauge(
            Metrics.createMqOldestMsgAge(meter), MetricsConfig::isMqOldestMsgAgeEnabled));
    gauges.put(
        CMQCFC.MQIACF_UNCOMMITTED_MSGS,
        createAllowedGauge(
            Metrics.createMqUncommittedMessages(meter),
            MetricsConfig::isMqUncommittedMessagesEnabled));
    gauges.put(
        CMQC.MQIA_MSG_DEQ_COUNT,
        createAllowedGauge(
            Metrics.createMqMessageDeqCount(meter), MetricsConfig::isMqMessageDeqCountEnabled));
    gauges.put(
        CMQC.MQIA_MSG_ENQ_COUNT,
        createAllowedGauge(
            Metrics.createMqMessageEnqCount(meter), MetricsConfig::isMqMessageEnqCountEnabled));
    gauges.put(
        CMQC.MQIA_HIGH_Q_DEPTH,
        createAllowedGauge(
            Metrics.createMqHighQueueDepth(meter), MetricsConfig::isMqHighQueueDepthEnabled));
    gauges.put(
        CMQCFC.MQIACF_CUR_Q_FILE_SIZE,
        createAllowedGauge(
            Metrics.createMqCurrentQueueFilesize(meter),
            MetricsConfig::isMqCurrentQueueFilesizeEnabled));
    gauges.put(
        CMQCFC.MQIACF_CUR_MAX_FILE_SIZE,
        createAllowedGauge(
            Metrics.createMqCurrentMaxQueueFilesize(meter),
            MetricsConfig::isMqCurrentMaxQueueFilesizeEnabled));

    this.onqtimeShort = Metrics.createMqOnqtime1(meter);
    this.onqtimeLong = Metrics.createMqOnqtime2(meter);
  }

  /**
   * Sends a PCFMessage request, reads the response, and generates metrics from the response. It
   * handles all exceptions.
   */
  void processPcfRequestAndPublishQMetrics(
      MetricsCollectorContext context, PCFMessage request, String queueGenericName, int[] fields) {
    try {
      doProcessPCFRequestAndPublishQMetrics(context, request, queueGenericName, fields);
    } catch (PCFException pcfe) {
      logger.error(
          "PCFException caught while collecting metric for Queue: {}", queueGenericName, pcfe);
      if (pcfe.exceptionSource instanceof PCFMessage[]) {
        PCFMessage[] msgs = (PCFMessage[]) pcfe.exceptionSource;
        for (PCFMessage msg : msgs) {
          logger.error(msg.toString());
        }
      }
      if (pcfe.exceptionSource instanceof PCFMessage) {
        PCFMessage msg = (PCFMessage) pcfe.exceptionSource;
        logger.error(msg.toString());
      }
      // Don't throw exception as it will stop queue metric collection
    } catch (Exception mqe) {
      logger.error("MQException caught", mqe);
      // Don't throw exception as it will stop queue metric collection
    }
  }

  private void doProcessPCFRequestAndPublishQMetrics(
      MetricsCollectorContext context, PCFMessage request, String queueGenericName, int[] fields)
      throws IOException, MQDataException {
    logger.debug(
        "sending PCF agent request to query metrics for generic queue {}", queueGenericName);
    long startTime = System.currentTimeMillis();
    List<PCFMessage> response = context.send(request);
    long endTime = System.currentTimeMillis() - startTime;
    logger.debug(
        "PCF agent queue metrics query response for generic queue {} received in {} milliseconds",
        queueGenericName,
        endTime);
    if (response.isEmpty()) {
      logger.debug("Unexpected error while PCFMessage.send(), response is empty");
      return;
    }

    List<PCFMessage> messages =
        MessageFilter.ofKind("queue")
            .excluding(context.getQueueExcludeFilters())
            .withResourceExtractor(MessageBuddy::queueName)
            .filter(response);

    for (PCFMessage message : messages) {
      handleMessage(context, message, fields);
    }
  }

  private void handleMessage(MetricsCollectorContext context, PCFMessage message, int[] fields)
      throws PCFException {
    String queueName = MessageBuddy.queueName(message);
    String queueType = getQueueTypeFromName(message, queueName);
    if (queueType == null) {
      logger.info("Unable to determine queue type for queue name = {}", queueName);
      return;
    }

    logger.debug("Pulling out metrics for queue name {}", queueName);
    getMetrics(context, message, queueName, queueType, fields);
  }

  private String getQueueTypeFromName(PCFMessage message, String queueName) throws PCFException {
    if (message.getParameterValue(CMQC.MQIA_Q_TYPE) == null) {
      return sharedState.getType(queueName);
    }

    String queueType = getQueueType(message);
    sharedState.putQueueType(queueName, queueType);
    return queueType;
  }

  private static String getQueueType(PCFMessage message) throws PCFException {
    String baseQueueType = getBaseQueueType(message);
    return maybeAppendUsage(message, baseQueueType);
  }

  private static String maybeAppendUsage(PCFMessage message, String baseQueueType)
      throws PCFException {
    if (message.getParameter(CMQC.MQIA_USAGE) == null) {
      return baseQueueType;
    }
    switch (message.getIntParameterValue(CMQC.MQIA_USAGE)) {
      case CMQC.MQUS_NORMAL:
        return baseQueueType + "-normal";
      case CMQC.MQUS_TRANSMISSION:
        return baseQueueType + "-transmission";
    }
    return baseQueueType;
  }

  private static String getBaseQueueType(PCFMessage message) throws PCFException {
    switch (message.getIntParameterValue(CMQC.MQIA_Q_TYPE)) {
      case MQQT_LOCAL:
        return "local";
      case MQQT_ALIAS:
        return "alias";
      case MQQT_REMOTE:
        return "remote";
      case MQQT_CLUSTER:
        return "cluster";
      case MQQT_MODEL:
        return "model";
    }
    logger.warn("Unknown type of queue {}", message.getIntParameterValue(CMQC.MQIA_Q_TYPE));
    return "unknown";
  }

  private void getMetrics(
      MetricsCollectorContext context,
      PCFMessage pcfMessage,
      String queueName,
      String queueType,
      int[] fields)
      throws PCFException {

    for (int field : fields) {
      if (field == CMQC.MQCA_Q_NAME || field == CMQC.MQIA_USAGE || field == CMQC.MQIA_Q_TYPE) {
        continue;
      }
      updateMetrics(context, pcfMessage, queueName, queueType, field);
    }
  }

  private void updateMetrics(
      MetricsCollectorContext context,
      PCFMessage pcfMessage,
      String queueName,
      String queueType,
      int constantValue)
      throws PCFException {
    PCFParameter pcfParam = pcfMessage.getParameter(constantValue);
    Attributes attributes =
        Attributes.of(
            AttributeKey.stringKey("queue.name"),
            queueName,
            AttributeKey.stringKey("queue.type"),
            queueType,
            AttributeKey.stringKey("queue.manager"),
            context.getQueueManagerName());

    if (pcfParam instanceof MQCFIN) {
      AllowedGauge g = this.gauges.get(constantValue);
      if (g == null) {
        throw new IllegalArgumentException("Unknown constantValue " + constantValue);
      }
      int metricVal = pcfMessage.getIntParameterValue(constantValue);
      g.set(context, metricVal, attributes);
    }
    if (pcfParam instanceof MQCFIL) {
      int[] metricVals = pcfMessage.getIntListParameterValue(constantValue);
      if (context.getMetricsConfig().isMqOnqtime1Enabled()) {
        onqtimeShort.set(metricVals[0], attributes);
      }
      if (context.getMetricsConfig().isMqOnqtime2Enabled()) {
        onqtimeLong.set(metricVals[1], attributes);
      }
    }
  }
}
