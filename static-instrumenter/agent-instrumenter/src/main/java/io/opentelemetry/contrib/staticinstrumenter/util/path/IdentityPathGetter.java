/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util.path;

import java.util.jar.JarEntry;

public class IdentityPathGetter implements AgentPathGetter {
  @Override
  public String getPath(JarEntry entry) {
    return entry.getName();
  }
}
