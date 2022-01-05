/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import java.io.File;
import java.lang.management.ManagementFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

public final class RuntimeAttach {

  public static void attachJavaagentToCurrentJVM() {
    if (agentIsDisabled()) {
      return;
    }
    File javaagentFile = AgentFileLocator.locateAgentFile();
    ByteBuddyAgent.attach(javaagentFile, getPid());
  }

  private static boolean agentIsDisabled() {
    String enabledProperty =
        System.getProperty("otel.javaagent.enabled", System.getenv("OTEL_JAVAAGENT_ENABLED"));
    return "false".equals(enabledProperty);
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
