/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq;

import static io.opentelemetry.ibm.mq.metrics.IbmMqAttributes.ERROR_CODE;
import static io.opentelemetry.ibm.mq.metrics.IbmMqAttributes.IBM_MQ_QUEUE_MANAGER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.Metrics;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import io.opentelemetry.ibm.mq.metricscollector.ChannelMetricsCollector;
import io.opentelemetry.ibm.mq.metricscollector.InquireChannelCmdCollector;
import io.opentelemetry.ibm.mq.metricscollector.InquireQueueManagerCmdCollector;
import io.opentelemetry.ibm.mq.metricscollector.ListenerMetricsCollector;
import io.opentelemetry.ibm.mq.metricscollector.MetricsCollectorContext;
import io.opentelemetry.ibm.mq.metricscollector.PerformanceEventQueueCollector;
import io.opentelemetry.ibm.mq.metricscollector.QueueManagerEventCollector;
import io.opentelemetry.ibm.mq.metricscollector.QueueManagerMetricsCollector;
import io.opentelemetry.ibm.mq.metricscollector.QueueMetricsCollector;
import io.opentelemetry.ibm.mq.metricscollector.ReadConfigurationEventQueueCollector;
import io.opentelemetry.ibm.mq.metricscollector.TopicMetricsCollector;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.ibm.mq.util.WmqUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WmqMonitor {

  private static final Logger logger = LoggerFactory.getLogger(WmqMonitor.class);

  private final List<QueueManager> queueManagers;
  private final List<Consumer<MetricsCollectorContext>> jobs = new ArrayList<>();
  private final LongCounter errorCodesCounter;
  private final LongGauge heartbeatGauge;
  private final ExecutorService threadPool;
  private final MetricsConfig metricsConfig;

  public WmqMonitor(ConfigWrapper config, ExecutorService threadPool, Meter meter) {
    List<Map<String, ?>> queueManagers = getQueueManagers(config);
    ObjectMapper mapper = new ObjectMapper();

    this.queueManagers = new ArrayList<>();

    for (Map<String, ?> queueManager : queueManagers) {
      try {
        QueueManager qManager = mapper.convertValue(queueManager, QueueManager.class);
        this.queueManagers.add(qManager);
      } catch (Throwable t) {
        logger.error("Error preparing queue manager {}", queueManager, t);
      }
    }

    this.metricsConfig = new MetricsConfig(config);

    this.heartbeatGauge = Metrics.createIbmMqHeartbeat(meter);
    this.errorCodesCounter = Metrics.createMqConnectionErrors(meter);
    this.threadPool = threadPool;

    jobs.add(new QueueManagerMetricsCollector(meter));
    jobs.add(new InquireQueueManagerCmdCollector(meter));
    jobs.add(new ChannelMetricsCollector(meter));
    jobs.add(new InquireChannelCmdCollector(meter));
    jobs.add(new QueueMetricsCollector(meter, threadPool, config));
    jobs.add(new ListenerMetricsCollector(meter));
    jobs.add(new TopicMetricsCollector(meter));
    jobs.add(new ReadConfigurationEventQueueCollector(meter));
    jobs.add(new PerformanceEventQueueCollector(meter));
    jobs.add(new QueueManagerEventCollector(meter));
  }

  public void run() {
    for (QueueManager qm : this.queueManagers) {
      run(qm);
    }
  }

  public void run(QueueManager queueManager) {
    String queueManagerName = queueManager.getName();
    logger.debug("WMQMonitor thread for queueManager {} started.", queueManagerName);
    long startTime = System.currentTimeMillis();
    MQQueueManager ibmQueueManager = null;
    PCFMessageAgent agent = null;
    int heartBeatMetricValue = 0;
    try {
      ibmQueueManager = WmqUtil.connectToQueueManager(queueManager);
      heartBeatMetricValue = 1;
      agent = WmqUtil.initPcfMessageAgent(queueManager, ibmQueueManager);
      extractAndReportMetrics(ibmQueueManager, queueManager, agent);
    } catch (RuntimeException e) {
      logger.error(
          "Error connecting to QueueManager {} by thread {}: {}",
          queueManagerName,
          Thread.currentThread().getName(),
          e.getMessage(),
          e);
      if (e.getCause() instanceof MQException) {
        MQException mqe = (MQException) e.getCause();
        String errorCode = String.valueOf(mqe.getReason());
        errorCodesCounter.add(
            1, Attributes.of(IBM_MQ_QUEUE_MANAGER, queueManagerName, ERROR_CODE, errorCode));
      }
    } finally {
      if (this.metricsConfig.isIbmMqHeartbeatEnabled()) {
        heartbeatGauge.set(
            heartBeatMetricValue, Attributes.of(IBM_MQ_QUEUE_MANAGER, queueManagerName));
      }
      cleanUp(ibmQueueManager, agent);
      long endTime = System.currentTimeMillis() - startTime;
      logger.debug(
          "WMQMonitor thread for queueManager {} ended. Time taken = {} ms",
          queueManagerName,
          endTime);
    }
  }

  @NotNull
  private static List<Map<String, ?>> getQueueManagers(ConfigWrapper config) {
    List<Map<String, ?>> queueManagers = config.getQueueManagers();
    if (queueManagers.isEmpty()) {
      throw new IllegalStateException(
          "The 'queueManagers' section in config.yml is empty or otherwise incorrect.");
    }
    return queueManagers;
  }

  private void extractAndReportMetrics(
      MQQueueManager mqQueueManager, QueueManager queueManager, PCFMessageAgent agent) {
    logger.debug("Queueing {} jobs", jobs.size());
    MetricsCollectorContext context =
        new MetricsCollectorContext(queueManager, agent, mqQueueManager, this.metricsConfig);
    List<Callable<Void>> tasks = new ArrayList<>();
    for (Consumer<MetricsCollectorContext> collector : jobs) {
      tasks.add(
          () -> {
            try {
              long startTime = System.currentTimeMillis();
              collector.accept(context);
              long diffTime = System.currentTimeMillis() - startTime;
              if (diffTime > 60000L) {
                logger.warn(
                    "{} Task took {} ms to complete",
                    collector.getClass().getSimpleName(),
                    diffTime);
              } else {
                logger.debug(
                    "{} Task took {} ms to complete",
                    collector.getClass().getSimpleName(),
                    diffTime);
              }
            } catch (RuntimeException e) {
              logger.error(
                  "Error while running task name = {}", collector.getClass().getSimpleName(), e);
            }
            return null;
          });
    }

    try {
      this.threadPool.invokeAll(tasks);
    } catch (InterruptedException e) {
      logger.error("Error while the thread {} is waiting ", Thread.currentThread().getName(), e);
    }
  }

  /** Destroy the agent and disconnect from queue manager */
  private static void cleanUp(
      @Nullable MQQueueManager ibmQueueManager, @Nullable PCFMessageAgent agent) {
    // Disconnect the agent.

    if (agent != null) {
      String qMgrName = agent.getQManagerName();
      try {
        agent.disconnect();
        logger.debug(
            "PCFMessageAgent disconnected for queueManager {} in thread {}",
            qMgrName,
            Thread.currentThread().getName());
      } catch (Exception e) {
        logger.error(
            "Error occurred  while disconnecting PCFMessageAgent for queueManager {} in thread {}",
            qMgrName,
            Thread.currentThread().getName(),
            e);
      }
    }

    // Disconnect queue manager
    if (ibmQueueManager != null) {
      String name = "";
      try {
        name = ibmQueueManager.getName();
        ibmQueueManager.disconnect();
      } catch (Exception e) {
        logger.error(
            "Error occurred while disconnecting queueManager {} in thread {}",
            name,
            Thread.currentThread().getName(),
            e);
      }
    }
  }
}
