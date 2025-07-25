/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.WmqMonitor;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * The TestWMQMonitor class extends the WMQMonitor class and provides a test implementation of the
 * WebSphere MQ monitoring functionality. It is intended for internal integration test purposes and
 * facilitates custom configuration through a test configuration file and a test metric write
 * helper.
 */
class TestWMQMonitor {

  private final ConfigWrapper config;
  private final ExecutorService threadPool;
  private final Meter meter;

  TestWMQMonitor(ConfigWrapper config, Meter meter, ExecutorService service) {
    this.config = config;
    this.threadPool = service;
    this.meter = meter;
  }

  /**
   * Executes a test run for monitoring WebSphere MQ queue managers based on the provided
   * configuration "testConfigFile".
   *
   * <p>The method retrieves "queueManagers" from the yml configuration file and uses a custom
   * MetricWriteHelper if provided, initializes a TasksExecutionServiceProvider, and executes the
   * WMQMonitorTask
   */
  void runTest() {
    List<Map<String, ?>> queueManagers = config.getQueueManagers();
    assertThat(queueManagers).isNotNull();
    ObjectMapper mapper = new ObjectMapper();

    WmqMonitor wmqTask = new WmqMonitor(config, threadPool, meter);

    // we override this helper to pass in our opentelemetry helper instead.
    for (Map<String, ?> queueManager : queueManagers) {
      QueueManager qManager = mapper.convertValue(queueManager, QueueManager.class);
      wmqTask.run(qManager);
    }
  }
}
