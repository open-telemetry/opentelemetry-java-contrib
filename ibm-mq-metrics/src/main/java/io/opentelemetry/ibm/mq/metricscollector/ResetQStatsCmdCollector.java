/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
