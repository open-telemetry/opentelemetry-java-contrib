/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class JarTestUtil {

  private JarTestUtil() {}

  static void assertJar(Path jarFile, String... files) throws IOException {
    assertThat(Files.exists(jarFile)).isTrue();
    try (JarFile jar = new JarFile(jarFile.toFile())) {
      for (int i = 0; i < files.length; i++) {
        String file = files[i];
        ZipEntry entry = jar.getEntry(file);
        assertThat(entry).isNotNull();
      }
    }
  }

  static String getResourcePath(String fileName) {
    return Thread.currentThread().getContextClassLoader().getResource(fileName).getFile();
  }

  static Path createJar(
      String prefix, JarSupport.ThrowingConsumer<ZipOutputStream, IOException> creator)
      throws IOException {
    Path path = Files.createTempFile(prefix, "jar");
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
      creator.accept(out);
    }
    return path;
  }
}
