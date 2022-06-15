/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;

/** This class allows you to attach the OpenTelemetry Java agent at runtime. */
public final class RuntimeAttach {

  private static final Logger logger = Logger.getLogger(RuntimeAttach.class.getName());
  private static final String AGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String AGENT_ENABLED_ENV_VAR = "OTEL_JAVAAGENT_ENABLED";
  static final String MAIN_THREAD_CHECK_PROP =
      "otel.javaagent.testing.runtime-attach.main-thread-check";

  /**
   * Attach the OpenTelemetry Java agent to the current JVM. The attachment must be requested at the
   * beginning of the main method.
   */
  public static void attachJavaagentToCurrentJVM() {
    if (!shouldAttach()) {
      return;
    }

    File javaagentFile = AgentFileLocator.locateAgentFile();
    ByteBuddyAgent.attach(javaagentFile, getPid());

    if (!agentIsAttached()) {
      logger.warning("Agent was not attached. An unexpected issue has happened.");
    }
  }

  private static boolean shouldAttach() {
    if (agentIsDisabledWithProp()) {
      logger.fine("Agent was disabled with " + AGENT_ENABLED_PROPERTY + " property.");
      return false;
    }
    if (agentIsDisabledWithEnvVar()) {
      logger.fine("Agent was disabled with " + AGENT_ENABLED_ENV_VAR + " environment variable.");
      return false;
    }
    if (agentIsAttached()) {
      logger.fine("Agent is already attached. It is not attached a second time.");
      return false;
    }
    if (mainThreadCheckIsEnabled() && !isMainThread()) {
      logger.warning(
          "Agent is not attached because runtime attachment was not requested from main thread.");
      return false;
    }
    return true;
  }

  private static boolean agentIsDisabledWithProp() {
    String agentEnabledPropValue = System.getProperty(AGENT_ENABLED_PROPERTY);
    return "false".equalsIgnoreCase(agentEnabledPropValue);
  }

  private static boolean agentIsDisabledWithEnvVar() {
    String agentEnabledEnvVarValue = System.getenv(AGENT_ENABLED_ENV_VAR);
    return "false".equals(agentEnabledEnvVarValue);
  }

  private static boolean agentIsAttached() {
    try {
      Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean mainThreadCheckIsEnabled() {
    String mainThreadCheck = System.getProperty(MAIN_THREAD_CHECK_PROP);
    return !"false".equals(mainThreadCheck);
  }

  private static boolean isMainThread() {
    Thread currentThread = Thread.currentThread();
    return "main".equals(currentThread.getName());
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
