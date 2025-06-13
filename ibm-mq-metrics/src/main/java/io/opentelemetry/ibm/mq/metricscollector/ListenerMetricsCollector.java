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

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ListenerMetricsCollector is a specialized implementation of the MetricsCollector that is
 * responsible for collecting and publishing metrics related to IBM MQ Listeners.
 *
 * <p>This class interacts with PCFMessageAgent to query metrics for specific listeners, applies
 * "include:" and "exclude:" listenerFilters defined in config yaml, and uses MetricWriteHelper to
 * publish the collected metrics in the required format.
 *
 * <p>Key functionalities include: • query using PCF Command: MQCMD_INQUIRE_LISTENER_STATUS to get
 * the status of one or more listeners on a queue manager. • retrieve tcp/ip listeners runtime
 * information such as: - listener is running or stopped - port number and transport type - last
 * error codes - associated command server •
 *
 * <p>It utilizes WMQMetricOverride to map metrics from the configuration to their IBM MQ constants.
 */
public final class ListenerMetricsCollector implements Consumer<MetricsCollectorContext> {

  private static final Logger logger = LoggerFactory.getLogger(ListenerMetricsCollector.class);
  private final LongGauge listenerStatusGauge;

  public ListenerMetricsCollector(Meter meter) {
    this.listenerStatusGauge = Metrics.createMqListenerStatus(meter);
  }

  @Override
  public void accept(MetricsCollectorContext context) {
    long entryTime = System.currentTimeMillis();

    int[] attrs = new int[] {CMQCFC.MQCACH_LISTENER_NAME, CMQCFC.MQIACH_LISTENER_STATUS};
    logger.debug(
        "Attributes being sent along PCF agent request to query channel metrics: "
            + Arrays.toString(attrs));

    Set<String> listenerGenericNames = context.getListenerIncludeFilterNames();
    for (String listenerGenericName : listenerGenericNames) {
      PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS);
      request.addParameter(CMQCFC.MQCACH_LISTENER_NAME, listenerGenericName);
      request.addParameter(CMQCFC.MQIACF_LISTENER_STATUS_ATTRS, attrs);
      try {
        logger.debug(
            "sending PCF agent request to query metrics for generic listener {}",
            listenerGenericName);
        long startTime = System.currentTimeMillis();
        List<PCFMessage> response = context.send(request);
        long endTime = System.currentTimeMillis() - startTime;
        logger.debug(
            "PCF agent listener metrics query response for generic listener {} received in {} milliseconds",
            listenerGenericName,
            endTime);
        if (response.isEmpty()) {
          logger.debug("Unexpected error while PCFMessage.send(), response is empty");
          return;
        }

        List<PCFMessage> messages =
            MessageFilter.ofKind("listener")
                .excluding(context.getListenerExcludeFilters())
                .withResourceExtractor(MessageBuddy::listenerName)
                .filter(response);

        for (PCFMessage message : messages) {
          String listenerName = MessageBuddy.listenerName(message);
          logger.debug("Pulling out metrics for listener name {}", listenerName);
          updateMetrics(message, listenerName, context);
        }
      } catch (Exception e) {
        logger.error(
            "Unexpected error while collecting metrics for listener " + listenerGenericName, e);
      }
    }
    long exitTime = System.currentTimeMillis() - entryTime;
    logger.debug("Time taken to publish metrics for all listener is {} milliseconds", exitTime);
  }

  private void updateMetrics(
      PCFMessage message, String listenerName, MetricsCollectorContext context)
      throws PCFException {
    if (context.getMetricsConfig().isMqListenerStatusEnabled()) {
      int status = message.getIntParameterValue(CMQCFC.MQIACH_LISTENER_STATUS);
      listenerStatusGauge.set(
          status,
          Attributes.of(
              AttributeKey.stringKey("listener.name"),
              listenerName,
              AttributeKey.stringKey("queue.manager"),
              context.getQueueManagerName()));
    }
  }
}
