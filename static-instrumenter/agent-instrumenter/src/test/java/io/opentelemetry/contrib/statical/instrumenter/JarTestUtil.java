/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.statical.instrumenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class JarTestUtil {

  private JarTestUtil() {}

  static String createJar(File tempDir, String fileName, String... files) throws IOException {

    File outFile = new File(tempDir, fileName);
    outFile.createNewFile();
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {

      for (String entry : files) {
        ZipEntry newEntry = new ZipEntry(entry);
        zos.putNextEntry(newEntry);
        zos.closeEntry();
      }
    }
    return outFile.getPath();
  }

  static void assertJar(File dir, String jarName, String[] files, byte[][] contents)
      throws IOException {
    File jarFile = new File(dir, jarName);
    assertThat(jarFile.exists()).isTrue();
    try (JarFile jar = new JarFile(jarFile)) {
      for (int i = 0; i < files.length; i++) {
        String file = files[i];
        byte[] content = contents[i];
        ZipEntry entry = jar.getEntry(file);
        assertThat(entry).isNotNull();
        if (content != null) {
          assertContent(jar, entry, content);
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
}
