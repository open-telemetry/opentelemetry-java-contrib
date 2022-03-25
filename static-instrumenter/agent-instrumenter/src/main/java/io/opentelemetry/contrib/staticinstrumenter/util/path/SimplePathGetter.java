/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util.path;

import java.util.jar.JarEntry;

public class SimplePathGetter implements AgentPathGetter {
  @Override
  public String getPath(JarEntry entry) {
    String name = entry.getName();
    if (name.startsWith("inst/")) {
      return name.replaceFirst("inst/", "").replaceAll("\\.classdata$", ".class");
    }
    return name;
  }
}
