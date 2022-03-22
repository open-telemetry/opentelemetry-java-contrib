/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

final class JarTestUtil {

  private JarTestUtil() {}

  static void assertJar(File dir, String jarName, String[] files, byte[][] contents)
      throws IOException {
    File jarFile = new File(dir, jarName);
    assertThat(jarFile.exists()).isTrue();
    try (JarFile jar = new JarFile(jarFile)) {
      for (int i = 0; i < files.length; i++) {
        String file = files[i];
        ZipEntry entry = jar.getEntry(file);
        assertThat(entry).isNotNull();
        if (contents != null && contents[i] != null) {
          assertContent(jar, entry, contents[i]);
        }
      }
    }
  }

  private static void assertContent(JarFile jar, ZipEntry entry, byte[] content)
      throws IOException {
    InputStream is = jar.getInputStream(entry);
    ByteArrayOutputStream baos = new ByteArrayOutputStream((int) entry.getSize());
    is.transferTo(baos);
    assertThat(content).isEqualTo(baos.toByteArray());
  }

  static String getResourcePath(String jarName) {
    return JarTestUtil.class.getClassLoader().getResource(jarName).getFile();
  }
}
