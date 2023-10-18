/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import io.opentelemetry.contrib.attach.core.CoreRuntimeAttach;

/** This class allows you to attach the OpenTelemetry Java agent at runtime. */
public final class RuntimeAttach {

  /**
   * Attach the OpenTelemetry Java agent to the current JVM. The attachment must be requested at the
   * beginning of the main method.
   */
  @SuppressWarnings("MemberName")
  public static void attachJavaagentToCurrentJvm() {

    CoreRuntimeAttach distroRuntimeAttach = new CoreRuntimeAttach("/otel-agent.jar");

    distroRuntimeAttach.attachJavaagentToCurrentJvm();
  }

  private RuntimeAttach() {}
}
