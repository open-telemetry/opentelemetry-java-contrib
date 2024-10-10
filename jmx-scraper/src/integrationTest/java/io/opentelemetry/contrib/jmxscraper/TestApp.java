/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

@SuppressWarnings("all")
public class TestApp implements TestAppMXBean {

  public static final String APP_STARTED_MSG = "app started";
  public static final String OBJECT_NAME = "io.opentelemetry.test:name=TestApp";

  private volatile boolean running;

  public static void main(String[] args) {
    TestApp app = TestApp.start();
    while (app.isRunning()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private TestApp() {}

  static TestApp start() {
    TestApp app = new TestApp();
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    try {
      ObjectName objectName = new ObjectName(OBJECT_NAME);
      mbs.registerMBean(app, objectName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    app.running = true;
    System.out.println(APP_STARTED_MSG);
    return app;
  }

  @Override
  public int getIntValue() {
    return 42;
  }

  @Override
  public void stopApp() {
    running = false;
  }

  boolean isRunning() {
    return running;
  }
}
