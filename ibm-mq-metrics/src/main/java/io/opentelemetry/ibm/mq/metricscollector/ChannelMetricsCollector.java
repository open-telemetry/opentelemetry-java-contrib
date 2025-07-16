/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static com.ibm.mq.constants.CMQC.MQRC_SELECTOR_ERROR;
import static com.ibm.mq.constants.CMQCFC.MQRCCF_CHL_STATUS_NOT_FOUND;

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is responsible for channel metric collection. */
public final class ChannelMetricsCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(ChannelMetricsCollector.class);

  private final LongGauge activeChannelsGauge;
  private final LongGauge channelStatusGauge;
  private final LongGauge messageCountGauge;
  private final LongGauge byteSentGauge;
  private final LongGauge byteReceivedGauge;
  private final LongGauge buffersSentGauge;
  private final LongGauge buffersReceivedGauge;
  private final LongGauge currentSharingConvsGauge;
  private final LongGauge maxSharingConvsGauge;

  /*
   * The Channel Status values are mentioned here http://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.dev.doc/q090880_.htm
   */
  public ChannelMetricsCollector(Meter meter) {
    this.activeChannelsGauge = Metrics.createIbmMqManagerActiveChannels(meter);
    this.channelStatusGauge = Metrics.createIbmMqStatus(meter);
    this.messageCountGauge = Metrics.createIbmMqMessageCount(meter);
    this.byteSentGauge = Metrics.createIbmMqByteSent(meter);
    this.byteReceivedGauge = Metrics.createIbmMqByteReceived(meter);
    this.buffersSentGauge = Metrics.createIbmMqBuffersSent(meter);
    this.buffersReceivedGauge = Metrics.createIbmMqBuffersReceived(meter);
    this.currentSharingConvsGauge = Metrics.createIbmMqCurrentSharingConversations(meter);
    this.maxSharingConvsGauge = Metrics.createIbmMqMaxSharingConversations(meter);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting metrics for command MQCMD_INQUIRE_CHANNEL_STATUS");
    long entryTime = System.currentTimeMillis();

    int[] attrs =
        new int[] {
          CMQCFC.MQCACH_CHANNEL_NAME,
          CMQCFC.MQCACH_CONNECTION_NAME,
          CMQCFC.MQIACH_CHANNEL_TYPE,
          CMQCFC.MQIACH_MSGS,
          CMQCFC.MQIACH_CHANNEL_STATUS,
          CMQCFC.MQIACH_BYTES_SENT,
          CMQCFC.MQIACH_BYTES_RECEIVED,
          CMQCFC.MQIACH_BUFFERS_SENT,
          CMQCFC.MQIACH_BUFFERS_RECEIVED,
          CMQCFC.MQIACH_CURRENT_SHARING_CONVS,
          CMQCFC.MQIACH_MAX_SHARING_CONVS,
          CMQCFC.MQCACH_CHANNEL_START_DATE,
          CMQCFC.MQCACH_CHANNEL_START_TIME,
          CMQCFC.MQCACH_MCA_JOB_NAME
        };
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Attributes being sent along PCF agent request to query channel metrics: {}",
          Arrays.toString(attrs));
    }

    Set<String> channelGenericNames = context.getChannelIncludeFilterNames();

    //
    // The MQCMD_INQUIRE_CHANNEL_STATUS command queries the current operational status of channels.
    // This includes information about whether a channel is running, stopped, or in another state,
    // as well as details about the channel’s performance and usage.
    List<String> activeChannels = new ArrayList<>();
    for (String channelGenericName : channelGenericNames) {
      PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS);
      request.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, channelGenericName);
      request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, CMQC.MQOT_CURRENT_CHANNEL);
      request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);
      try {
        logger.debug(
            "sending PCF agent request to query metrics for generic channel {}",
            channelGenericName);
        long startTime = System.currentTimeMillis();
        List<PCFMessage> response = context.send(request);
        long endTime = System.currentTimeMillis() - startTime;
        logger.debug(
            "PCF agent queue metrics query response for generic queue {} received in {} milliseconds",
            channelGenericName,
            endTime);
        if (response.isEmpty()) {
          logger.debug("Unexpected error while PCFMessage.send(), response is empty");
          return;
        }

        List<PCFMessage> messages =
            MessageFilter.ofKind("channel")
                .excluding(context.getChannelExcludeFilters())
                .withResourceExtractor(MessageBuddy::channelName)
                .filter(response);

        for (PCFMessage message : messages) {
          String channelName = MessageBuddy.channelName(message);
          String channelType = MessageBuddy.channelType(message);
          long channelStartTime = MessageBuddy.channelStartTime(message);
          String jobName = MessageBuddy.jobName(message);

          logger.debug("Pulling out metrics for channel name {}", channelName);
          updateMetrics(
              context,
              message,
              channelName,
              channelType,
              channelStartTime,
              jobName,
              activeChannels);
        }
      } catch (PCFException pcfe) {
        if (pcfe.getReason() == MQRCCF_CHL_STATUS_NOT_FOUND) {
          String errorMsg = "Channel- " + channelGenericName + " :";
          errorMsg +=
              "Could not collect channel information as channel is stopped or inactive: Reason '3065'\n";
          errorMsg +=
              "If the channel type is MQCHT_RECEIVER, MQCHT_SVRCONN or MQCHT_CLUSRCVR, then the only action is to enable the channel, not start it.";
          logger.error(errorMsg, pcfe);
        } else if (pcfe.getReason() == MQRC_SELECTOR_ERROR) {
          logger.error(
              "Invalid metrics passed while collecting channel metrics, check config.yaml: Reason '2067'",
              pcfe);
        } else {
          logger.error(pcfe.getMessage(), pcfe);
        }
      } catch (Exception e) {
        logger.error(
            "Unexpected error occurred while collecting metrics for channel " + channelGenericName,
            e);
      }
    }

    logger.info(
        "Active Channels in queueManager {} are {}", context.getQueueManagerName(), activeChannels);
    activeChannelsGauge.set(
        activeChannels.size(),
        Attributes.of(AttributeKey.stringKey("queue.manager"), context.getQueueManagerName()));

    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug("Time taken to publish metrics for all channels is {} milliseconds", exitTime);
  }

  private void updateMetrics(
      MetricsCollectorContext context,
      PCFMessage message,
      String channelName,
      String channelType,
      long channelStartTime,
      String jobName,
      List<String> activeChannels)
      throws PCFException {
    Attributes attributes =
        Attributes.builder()
            .put("channel.name", channelName)
            .put("channel.type", channelType)
            .put("queue.manager", context.getQueueManagerName())
            .put("channel.start.time", channelStartTime)
            .put("job.name", jobName)
            .build();
    if (context.getMetricsConfig().isIbmMqMessageCountEnabled()) {
      int received = message.getIntParameterValue(CMQCFC.MQIACH_MSGS);
      messageCountGauge.set(received, attributes);
    }
    int status = message.getIntParameterValue(CMQCFC.MQIACH_CHANNEL_STATUS);
    if (context.getMetricsConfig().isIbmMqStatusEnabled()) {
      channelStatusGauge.set(status, attributes);
    }
    // We follow the definition of active channel as documented in
    // https://www.ibm.com/docs/en/ibm-mq/9.2.x?topic=states-current-active
    if (status != CMQCFC.MQCHS_RETRYING
        && status != CMQCFC.MQCHS_STOPPED
        && status != CMQCFC.MQCHS_STARTING) {
      activeChannels.add(channelName);
    }
    if (context.getMetricsConfig().isIbmMqByteSentEnabled()) {
      byteSentGauge.set(message.getIntParameterValue(CMQCFC.MQIACH_BYTES_SENT), attributes);
    }
    if (context.getMetricsConfig().isIbmMqByteReceivedEnabled()) {
      byteReceivedGauge.set(message.getIntParameterValue(CMQCFC.MQIACH_BYTES_RECEIVED), attributes);
    }
    if (context.getMetricsConfig().isIbmMqBuffersSentEnabled()) {
      buffersSentGauge.set(message.getIntParameterValue(CMQCFC.MQIACH_BUFFERS_SENT), attributes);
    }
    if (context.getMetricsConfig().isIbmMqBuffersReceivedEnabled()) {
      buffersReceivedGauge.set(
          message.getIntParameterValue(CMQCFC.MQIACH_BUFFERS_RECEIVED), attributes);
    }
    if (context.getMetricsConfig().isIbmMqCurrentSharingConversationsEnabled()) {
      int currentSharingConvs = 0;
      if (message.getParameter(CMQCFC.MQIACH_CURRENT_SHARING_CONVS) != null) {
        currentSharingConvs = message.getIntParameterValue(CMQCFC.MQIACH_CURRENT_SHARING_CONVS);
      }
      currentSharingConvsGauge.set(currentSharingConvs, attributes);
    }
    if (context.getMetricsConfig().isIbmMqMaxSharingConversationsEnabled()) {
      int maxSharingConvs = 0;
      if (message.getParameter(CMQCFC.MQIACH_MAX_SHARING_CONVS) != null) {
        maxSharingConvs = message.getIntParameterValue(CMQCFC.MQIACH_MAX_SHARING_CONVS);
      }
      maxSharingConvsGauge.set(maxSharingConvs, attributes);
    }
  }
}
