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
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResetQStatsCmdCollector implements Consumer<MetricsCollectorContext> {

  static final int[] ATTRIBUTES =
      new int[] {CMQC.MQIA_HIGH_Q_DEPTH, CMQC.MQIA_MSG_DEQ_COUNT, CMQC.MQIA_MSG_ENQ_COUNT};

  private static final Logger logger = LoggerFactory.getLogger(ResetQStatsCmdCollector.class);

  static final String COMMAND = "MQCMD_RESET_Q_STATS";
  private final QueueCollectionBuddy queueBuddy;

  ResetQStatsCmdCollector(QueueCollectionBuddy queueBuddy) {
    this.queueBuddy = queueBuddy;
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    logger.info("Collecting metrics for command {}", COMMAND);
    long entryTime = System.currentTimeMillis();

    logger.debug(
        "Attributes being sent along PCF agent request to query queue metrics: {} for command {}",
        Arrays.toString(ATTRIBUTES),
        COMMAND);

    Set<String> queueGenericNames = context.getQueueIncludeFilterNames();
    for (String queueGenericName : queueGenericNames) {
      // list of all metrics extracted through MQCMD_RESET_Q_STATS is mentioned here
      // https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q088310_.htm
      PCFMessage request = new PCFMessage(CMQCFC.MQCMD_RESET_Q_STATS);
      request.addParameter(CMQC.MQCA_Q_NAME, queueGenericName);
      queueBuddy.processPCFRequestAndPublishQMetrics(
          context, request, queueGenericName, ATTRIBUTES);
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug(
        "Time taken to publish metrics for all queues is {} milliseconds for command {}",
        exitTime,
        COMMAND);
  }
}
