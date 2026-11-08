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
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricProducer;
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
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Connects to the configured IBM MQ queue managers and runs the metric collectors against each. */
public final class WmqMonitor {

  private static final Logger logger = LoggerFactory.getLogger(WmqMonitor.class);

  private static final long CANCELLATION_GRACE_SECONDS = 30;

  private final Map<QueueManager, MetricProducer> queueManagers;
  private final List<Consumer<MetricsCollectorContext>> jobs = new ArrayList<>();
  private final MetricsConfig metricsConfig;

  public WmqMonitor(ConfigWrapper config) {
    List<Map<String, ?>> queueManagers = getMetricProducersByQueueManagers(config);
    ObjectMapper mapper = new ObjectMapper();

    this.queueManagers = new HashMap<>();

    for (Map<String, ?> queueManager : queueManagers) {
      try {
        QueueManager qManager = mapper.convertValue(queueManager, QueueManager.class);
        this.queueManagers.put(
            qManager,
            new MetricProducer(Resource.empty(), InstrumentationScopeInfo.create("websphere/mq")));
      } catch (Throwable t) {
        logger.error("Error preparing queue manager {}", queueManager, t);
      }
    }

    this.metricsConfig = new MetricsConfig(config);

    jobs.add(new QueueManagerMetricsCollector());
    jobs.add(new InquireQueueManagerCmdCollector());
    jobs.add(new ChannelMetricsCollector());
    jobs.add(new InquireChannelCmdCollector());
    jobs.add(new QueueMetricsCollector());
    jobs.add(new ListenerMetricsCollector());
    jobs.add(new TopicMetricsCollector());
    jobs.add(new ReadConfigurationEventQueueCollector());
    jobs.add(new PerformanceEventQueueCollector());
    jobs.add(new QueueManagerEventCollector());
  }

  public void run(
      QueueManager queueManager,
      MetricProducer producer,
      ExecutorService threadPool,
      @Nullable Integer timeoutSeconds) {
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
      extractAndReportMetrics(
          ibmQueueManager, queueManager, agent, producer, threadPool, timeoutSeconds);
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
        producer.addIbmMqConnectionErrors(
            1, Attributes.of(IBM_MQ_QUEUE_MANAGER, queueManagerName, ERROR_CODE, errorCode));
      }
    } finally {
      if (this.metricsConfig.isIbmMqHeartbeatEnabled()) {
        producer.recordIbmMqHeartbeat(
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

  public Map<QueueManager, MetricProducer> getMetricProducersByQueueManagers() {
    return this.queueManagers;
  }

  @NotNull
  private static List<Map<String, ?>> getMetricProducersByQueueManagers(ConfigWrapper config) {
    List<Map<String, ?>> queueManagers = config.getQueueManagers();
    if (queueManagers.isEmpty()) {
      throw new IllegalStateException(
          "The 'queueManagers' section in config.yml is empty or otherwise incorrect.");
    }
    return queueManagers;
  }

  private void extractAndReportMetrics(
      MQQueueManager mqQueueManager,
      QueueManager queueManager,
      PCFMessageAgent agent,
      MetricProducer producer,
      ExecutorService threadPool,
      @Nullable Integer timeoutSeconds) {
    logger.debug("Queueing {} jobs", jobs.size());
    MetricsCollectorContext context =
        new MetricsCollectorContext(
            queueManager, agent, mqQueueManager, this.metricsConfig, producer);
    CountDownLatch finished = new CountDownLatch(jobs.size());
    List<CollectorTask> tasks = new ArrayList<>();
    for (Consumer<MetricsCollectorContext> collector : jobs) {
      tasks.add(
          new CollectorTask(
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
                      "Error while running task name = {}",
                      collector.getClass().getSimpleName(),
                      e);
                }
              },
              finished));
    }
    try {
      if (timeoutSeconds != null) {
        threadPool.invokeAll(tasks, timeoutSeconds, TimeUnit.SECONDS);
      } else {
        threadPool.invokeAll(tasks);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Interrupted while collecting metrics for {}", queueManager.getName(), e);
    }
    awaitRunningCollectors(queueManager, tasks, finished);
  }

  // invokeAll only cancels (interrupts) unfinished futures; it does not wait for the collectors to
  // react. Wait for the ones that actually started before the caller closes the connections.
  private static void awaitRunningCollectors(
      QueueManager queueManager, List<CollectorTask> tasks, CountDownLatch finished) {
    for (CollectorTask task : tasks) {
      task.cancelIfNotStarted();
    }
    boolean interrupted = Thread.interrupted();
    try {
      if (!finished.await(CANCELLATION_GRACE_SECONDS, TimeUnit.SECONDS)) {
        logger.warn(
            "{} of {} collectors for queue manager {} did not stop within {}s of cancellation",
            finished.getCount(),
            tasks.size(),
            queueManager.getName(),
            CANCELLATION_GRACE_SECONDS);
      }
    } catch (InterruptedException e) {
      interrupted = true;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
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
