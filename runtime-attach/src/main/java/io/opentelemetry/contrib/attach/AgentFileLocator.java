/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;

final class AgentFileLocator {

  static File locateAgentFile() {
    CodeSource codeSource = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource();

    if (codeSource == null) {
      throw new IllegalStateException("could not get agent jar location");
    }

    URL codeSourceLocation = codeSource.getLocation();
    try {
      File javaagentFile = new File(codeSourceLocation.toURI());
      if (javaagentFile.isFile()) {
        return javaagentFile;
      }
    } catch (URISyntaxException ignored) {
      // ignored
    }

    throw new IllegalStateException(
        "agent jar location doesn't appear to be a file: " + codeSourceLocation);
  }

  private AgentFileLocator() {}
}
