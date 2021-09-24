/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import java.io.File;
import java.lang.management.ManagementFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

public class RuntimeAttach {
  public static void attachJavaagentToCurrentJVM() {
    File javaagentFile = AgentFileLocator.locateAgentFile();
    ByteBuddyAgent.attach(javaagentFile, getPid());
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }
}
