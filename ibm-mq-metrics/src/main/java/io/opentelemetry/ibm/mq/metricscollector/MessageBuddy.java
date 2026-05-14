/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.CMQXC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class MessageBuddy {

  private MessageBuddy() {}

  static String channelName(PCFMessage message) throws PCFException {
    return message.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME).trim();
  }

  static String channelType(PCFMessage message) throws PCFException {
    switch (message.getIntParameterValue(CMQCFC.MQIACH_CHANNEL_TYPE)) {
      case CMQXC.MQCHT_SENDER:
        return "sender";
      case CMQXC.MQCHT_SERVER:
        return "server";
      case CMQXC.MQCHT_RECEIVER:
        return "receiver";
      case CMQXC.MQCHT_REQUESTER:
        return "requester";
      case CMQXC.MQCHT_SVRCONN:
        return "server-connection";
      case CMQXC.MQCHT_CLNTCONN:
        return "client-connection";
      case CMQXC.MQCHT_CLUSRCVR:
        return "cluster-receiver";
      case CMQXC.MQCHT_CLUSSDR:
        return "cluster-sender";
      case CMQXC.MQCHT_MQTT:
        return "mqtt";
      case CMQXC.MQCHT_AMQP:
        return "amqp";
      default:
        throw new IllegalArgumentException(
            "Unsupported channel type: "
                + message.getIntParameterValue(CMQCFC.MQIACH_CHANNEL_TYPE));
    }
  }

  static String topicName(PCFMessage message) throws PCFException {
    return message.getStringParameterValue(CMQC.MQCA_TOPIC_STRING).trim();
  }

  public static String listenerName(PCFMessage message) throws PCFException {
    return message.getStringParameterValue(CMQCFC.MQCACH_LISTENER_NAME).trim();
  }

  public static String queueName(PCFMessage message) throws PCFException {
    return message.getStringParameterValue(CMQC.MQCA_Q_NAME).trim();
  }

  public static long channelStartTime(PCFMessage message) throws PCFException {
    String date = message.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_START_DATE).trim();
    String time = message.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_START_TIME).trim();

    Instant parsed = Instant.parse(date + "T" + time.replaceAll("\\.", ":") + "Z");
    return parsed.getEpochSecond();
  }

  public static String jobName(PCFMessage message) throws PCFException {
    return message.getStringParameterValue(CMQCFC.MQCACH_MCA_JOB_NAME).trim();
  }

  /**
   * Calculate the queue manager uptime in seconds.
   *
   * <p>Fetches the queue manager start date and time from the PCF response, parses them, and
   * calculates the difference from the current system time. The start time is assumed to be in the
   * system's default timezone.
   *
   * @param message the PCF response message containing queue manager information
   * @return uptime in seconds since the queue manager started
   * @throws PCFException if the required attributes cannot be retrieved from the PCF message
   */
  public static long queueManagerUptime(PCFMessage message) throws PCFException {
    // Note: MQCA_Q_MGR_START_DATE (3175) and MQCA_Q_MGR_START_TIME (3176) may not have
    // constant definitions in all IBM MQ client versions, so we use raw parameter IDs
    String date = message.getStringParameterValue(3175).trim();
    String time = message.getStringParameterValue(3176).trim();

    // Parse the date (format: yyyy-MM-dd) and time (format: HH.mm.ss)
    // Assumes the QM start time is in the system's default timezone (typically where the
    // collector is running)
    LocalDateTime qmgrStartLocal = LocalDateTime.parse(date + "T" + time.replaceAll("\\.", ":"));
    Instant qmgrStartInstant = qmgrStartLocal.atZone(ZoneId.systemDefault()).toInstant();
    Instant now = Instant.now();

    // Calculate uptime in seconds
    return now.getEpochSecond() - qmgrStartInstant.getEpochSecond();
  }
}
