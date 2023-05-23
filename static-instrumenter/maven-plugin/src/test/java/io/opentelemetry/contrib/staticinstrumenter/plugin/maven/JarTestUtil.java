/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class JarTestUtil {

  private JarTestUtil() {}

  static void assertJarContainsFiles(Path jarFile, String... files) throws IOException {
    assertThat(Files.exists(jarFile)).isTrue();
    try (JarFile jar = new JarFile(jarFile.toFile())) {
      for (int i = 0; i < files.length; i++) {
        String file = files[i];
        ZipEntry entry = jar.getEntry(file);
        assertThat(entry).isNotNull();
      }
    }
  }

  static Path getResourcePath(String fileName) {
    try {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
      if (resource == null) {
        throw new RuntimeException("Resource not found for " + fileName);
      }
      URI uri = resource.toURI();
      return Path.of(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Problem to find the path of " + fileName, e);
    }
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
