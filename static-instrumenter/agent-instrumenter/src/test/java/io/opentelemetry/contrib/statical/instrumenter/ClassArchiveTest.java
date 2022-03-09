/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.statical.instrumenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ClassArchiveTest {

  @TempDir private File tempDir;

  @Test
  void shouldCopyAllClasses(@TempDir File destination) throws IOException {

    // given
    byte[] newTransformed = new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
    Map<String, byte[]> transformed = Collections.singletonMap("test/TestClass", newTransformed);
    String jar = JarTestUtil.createJar(tempDir, "one.jar", "test/TestClass.class", "second.class");
    ClassArchive underTest = new ClassArchive(new JarFile(jar), transformed);

    // when
    File jarOut = new File(destination, "one.jar");
    try (ZipOutputStream zout = zout(jarOut)) {
      underTest.copyAllClassesTo(zout);
    }
    JarTestUtil.assertJar(
        destination,
        "one.jar",
        new String[] {"test/TestClass.class", "second.class"},
        new byte[][] {newTransformed, null});
  }

  private static ZipOutputStream zout(File destination) throws IOException {
    return new ZipOutputStream(new FileOutputStream(destination));
  }
}
