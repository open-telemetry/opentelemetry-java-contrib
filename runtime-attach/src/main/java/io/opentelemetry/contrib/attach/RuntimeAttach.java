/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import java.io.File;
import java.lang.management.ManagementFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

/** This class allows you to attach the OpenTelemetry Java agent at runtime. */
public final class RuntimeAttach {

  private static final String AGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String AGENT_ENABLED_ENV_VAR = "OTEL_JAVAAGENT_ENABLED";
  static final String MAIN_METHOD_CHECK_PROP =
      "otel.javaagent.testing.runtime-attach.main-method-check";

  /**
   * Attach the OpenTelemetry Java agent to the current JVM. The attachment must be requested at the
   * beginning of the main method.
   */
  public static void attachJavaagentToCurrentJVM() {
    if (!shouldAttach()) {
      return;
    }

    File javaagentFile = AgentFileProvider.getAgentFile();
    ByteBuddyAgent.attach(javaagentFile, getPid());

    if (!agentIsAttached()) {
      printError("Agent was not attached. An unexpected issue has happened.");
    }
  }

  @SuppressWarnings("SystemOut")
  private static void printError(String message) {
    // not using java.util.logging in order to avoid initializing the global LogManager
    // too early (and incompatibly with the user's app),
    // and because this is too early to use the Javaagent's PatchLogger
    System.err.println(message);
  }

  private static boolean shouldAttach() {
    if (agentIsDisabledWithProp()) {
      return false;
    }
    if (agentIsDisabledWithEnvVar()) {
      return false;
    }
    if (agentIsAttached()) {
      return false;
    }
    if (mainMethodCheckIsEnabled() && !isMainThread()) {
      printError(
          "Agent is not attached because runtime attachment was not requested from main thread.");
      return false;
    }
    if (mainMethodCheckIsEnabled() && !isMainMethod()) {
      printError(
          "Agent is not attached because runtime attachment was not requested from main method.");
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

  private static boolean mainMethodCheckIsEnabled() {
    String mainThreadCheck = System.getProperty(MAIN_METHOD_CHECK_PROP);
    return !"false".equals(mainThreadCheck);
  }

  private static boolean isMainThread() {
    Thread currentThread = Thread.currentThread();
    return "main".equals(currentThread.getName());
  }

  static boolean isMainMethod() {
    StackTraceElement bottomOfStack = findBottomOfStack(Thread.currentThread());
    String methodName = bottomOfStack.getMethodName();
    return "main".equals(methodName);
  }

  private static StackTraceElement findBottomOfStack(Thread thread) {
    StackTraceElement[] stackTrace = thread.getStackTrace();
    return stackTrace[stackTrace.length - 1];
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
