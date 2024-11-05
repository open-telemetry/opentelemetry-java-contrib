/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class TestApp implements TestAppMXBean {

  private volatile boolean running;

  @SuppressWarnings("BusyWait")
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
      ObjectName objectName = new ObjectName("io.opentelemetry.test:name=TestApp");
      mbs.registerMBean(app, objectName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    app.running = true;
    System.out.println("app started");
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
