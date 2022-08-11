/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class JarSupport {

  private JarSupport() {}

  @FunctionalInterface
  interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;
  }

  static void consumeEntries(JarFile jarFile, ThrowingConsumer<JarEntry, IOException> consumer)
      throws IOException {
    Enumeration<JarEntry> enums = jarFile.entries();
    while (enums.hasMoreElements()) {
      JarEntry entry = enums.nextElement();
      consumer.accept(entry);
    }
  }
}
