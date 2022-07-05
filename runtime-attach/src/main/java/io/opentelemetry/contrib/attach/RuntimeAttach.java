/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

/** This class allows you to attach the OpenTelemetry Java agent at runtime. */
public final class RuntimeAttach {

  /**
   * Attach the OpenTelemetry Java agent to the current JVM. The attachment must be requested at the
   * beginning of the main method.
   */
  public static void attachJavaagentToCurrentJVM() {

    DistroRuntimeAttach distroRuntimeAttach = new DistroRuntimeAttach("/otel-agent.jar");

    distroRuntimeAttach.attachJavaagentToCurrentJVM();
  }

  private RuntimeAttach() {}
}
