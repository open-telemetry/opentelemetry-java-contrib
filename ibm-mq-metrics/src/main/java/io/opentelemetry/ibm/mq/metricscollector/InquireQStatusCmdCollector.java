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
import com.ibm.mq.headers.pcf.PCFMessage;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The InquireQStatusCmdCollector class is responsible for collecting and publishing queue metrics
 * using the IBM MQ command `MQCMD_INQUIRE_Q_STATUS`. It extends the QueueMetricsCollector class and
 * implements the Runnable interface, enabling execution within a separate thread.
 *
 * <p>This class interacts with PCF (Programmable Command Formats) messages to query queue metrics
 * based on the configuration provided. It retrieves status information about a queue, such as: •
 * The number of messages on the queue • Open handles (how many apps have it open) • Whether the
 * queue is in use for input/output • Last get/put timestamps • And other real-time statistics
 *
 * <p>Thread Safety: This class is thread-safe, as it operates independently with state shared only
 * through immutable or synchronized structures where necessary.
 *
 * <p>Usage: - Instantiate this class by providing an existing QueueMetricsCollector instance, a map
 * of metrics to report, and shared state. - Invoke the run method to execute the queue metrics
 * collection process.
 */
final class InquireQStatusCmdCollector implements Consumer<MetricsCollectorContext> {

  static final int[] ATTRIBUTES =
      new int[] {
        CMQC.MQCA_Q_NAME,
        CMQCFC.MQIACF_CUR_Q_FILE_SIZE,
        CMQCFC.MQIACF_CUR_MAX_FILE_SIZE,
        CMQCFC.MQIACF_OLDEST_MSG_AGE,
        CMQCFC.MQIACF_UNCOMMITTED_MSGS,
        CMQCFC.MQIACF_Q_TIME_INDICATOR,
        CMQC.MQIA_CURRENT_Q_DEPTH,
      };

  private static final Logger logger = LoggerFactory.getLogger(InquireQStatusCmdCollector.class);

  private final QueueCollectionBuddy queueBuddy;

  InquireQStatusCmdCollector(QueueCollectionBuddy queueBuddy) {
    this.queueBuddy = queueBuddy;
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting metrics for command MQCMD_INQUIRE_Q_STATUS");
    long entryTime = System.currentTimeMillis();

    Set<String> queueGenericNames = context.getQueueIncludeFilterNames();
    for (String queueGenericName : queueGenericNames) {
      // list of all metrics extracted through MQCMD_INQUIRE_Q_STATUS is mentioned here
      // https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q087880_.htm
      PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_STATUS);
      request.addParameter(CMQC.MQCA_Q_NAME, queueGenericName);
      request.addParameter(CMQCFC.MQIACF_Q_STATUS_ATTRS, ATTRIBUTES);
      queueBuddy.processPCFRequestAndPublishQMetrics(
          context, request, queueGenericName, ATTRIBUTES);
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug(
        "Time taken to publish metrics for all queues is {} milliseconds for command MQCMD_INQUIRE_Q_STATUS",
        exitTime);
  }
}
