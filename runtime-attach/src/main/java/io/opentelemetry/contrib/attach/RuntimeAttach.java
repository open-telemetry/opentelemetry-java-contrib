/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;

public final class RuntimeAttach {

  private static final Logger LOGGER = Logger.getLogger(RuntimeAttach.class.getName());
  private static final String AGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String AGENT_ENABLED_ENV_VAR = "OTEL_JAVAAGENT_ENABLED";

  public static void attachJavaagentToCurrentJVM() {
    if (!shouldAttach()) {
      return;
    }
    File javaagentFile = AgentFileLocator.locateAgentFile();
    ByteBuddyAgent.attach(javaagentFile, getPid());
  }

  private static boolean shouldAttach() {
    if (agentIsDisabledWithProp()) {
      LOGGER.warning("Agent was disabled with " + AGENT_ENABLED_PROPERTY + " property.");
      return false;
    }
    if (agentIsDisabledWithEnvVar()) {
      LOGGER.warning("Agent was disabled with " + AGENT_ENABLED_ENV_VAR + " environment variable.");
      return false;
    }
    return true;
  }

  private static boolean agentIsDisabledWithProp() {
    String agentEnabledPropValue = System.getProperty(AGENT_ENABLED_PROPERTY);
    return "false".equals(agentEnabledPropValue);
  }

  private static boolean agentIsDisabledWithEnvVar() {
    String agentEnabledEnvVarValue = System.getenv(AGENT_ENABLED_ENV_VAR);
    return "false".equals(agentEnabledEnvVarValue);
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
