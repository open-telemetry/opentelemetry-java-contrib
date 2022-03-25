/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util.path;

import java.util.jar.JarEntry;

/** Supplies a new path of a given entry inside modified agent JAR. */
public interface AgentPathGetter {
  String getPath(JarEntry entry);
}
