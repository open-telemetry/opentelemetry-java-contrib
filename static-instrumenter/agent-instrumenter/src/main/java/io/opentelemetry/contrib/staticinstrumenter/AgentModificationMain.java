/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import java.io.File;

public final class AgentModificationMain {

  /**
   * The main method of agent-instrumenter module. It begins the process of instrumentation of the
   * agent.
   *
   * @param args First argument is the path to the OpenTelemetry javaagent JAR file. Second argument
   *     is the path to the output folder, where resulting modified agent JARs will be placed.
   */
  public static void main(String[] args) {

    String otelJarPath;
    String outDirPath;

    if (args.length >= 2) {
      otelJarPath = args[0];
      outDirPath = args[1];
    } else {
      otelJarPath = "opentelemetry-javaagent.jar";
      outDirPath = "outjars";
    }

    File outDir = new File(outDirPath);
    if (!outDir.exists()) {
      outDir.mkdir();
    }

    AgentInstrumenter agentInstrumenter = new AgentInstrumenter(otelJarPath, outDirPath);
    agentInstrumenter.instrument();
  }

  private AgentModificationMain() {}
}
