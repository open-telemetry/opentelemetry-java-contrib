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
import com.ibm.mq.constants.CMQXC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import java.time.Instant;

public class MessageBuddy {

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
}
