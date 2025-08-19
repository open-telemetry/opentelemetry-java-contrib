/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static java.util.Collections.emptyList;

import com.ibm.mq.MQQueueManager;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.ibm.mq.config.ExcludeFilters;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.jetbrains.annotations.NotNull;

/**
 * A temporary bundle to contain the collaborators of the original MetricsCollector base class until
 * we can finish unwinding things. When done and there are no longer usages of MetricsCollector, we
 * could consider renaming this.
 */
@Immutable
public final class MetricsCollectorContext {

  private final QueueManager queueManager;
  private final PCFMessageAgent agent;
  private final MQQueueManager mqQueueManager;
  private final MetricsConfig metricsConfig;

  public MetricsCollectorContext(
      QueueManager queueManager,
      PCFMessageAgent agent,
      MQQueueManager mqQueueManager,
      MetricsConfig metricsConfig) {
    this.queueManager = queueManager;
    this.agent = agent;
    this.mqQueueManager = mqQueueManager;
    this.metricsConfig = metricsConfig;
  }

  Set<String> getChannelIncludeFilterNames() {
    return queueManager.getChannelFilters().getInclude();
  }

  Set<ExcludeFilters> getChannelExcludeFilters() {
    return queueManager.getChannelFilters().getExclude();
  }

  Set<String> getListenerIncludeFilterNames() {
    return queueManager.getListenerFilters().getInclude();
  }

  Set<ExcludeFilters> getListenerExcludeFilters() {
    return queueManager.getListenerFilters().getExclude();
  }

  Set<String> getTopicIncludeFilterNames() {
    return queueManager.getTopicFilters().getInclude();
  }

  Set<ExcludeFilters> getTopicExcludeFilters() {
    return queueManager.getTopicFilters().getExclude();
  }

  Set<String> getQueueIncludeFilterNames() {
    return queueManager.getQueueFilters().getInclude();
  }

  Set<ExcludeFilters> getQueueExcludeFilters() {
    return queueManager.getQueueFilters().getExclude();
  }

  @NotNull
  List<PCFMessage> send(PCFMessage request) throws IOException, MQDataException {
    PCFMessage[] result = agent.send(request);
    return result == null ? emptyList() : Arrays.asList(result);
  }

  String getQueueManagerName() {
    return queueManager.getName();
  }

  QueueManager getQueueManager() {
    return queueManager;
  }

  public MQQueueManager getMqQueueManager() {
    return mqQueueManager;
  }

  public MetricsConfig getMetricsConfig() {
    return metricsConfig;
  }
}
