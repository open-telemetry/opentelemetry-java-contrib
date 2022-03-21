/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ClassArchiveTest {

  @TempDir private File tempDir;

  @Test
  void shouldCopyAllClasses(@TempDir File destination) throws IOException {

    // given
    byte[] newTransformed = new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
    Map<String, byte[]> transformed = Collections.singletonMap("test/TestClass", newTransformed);
    String jar =
        JarTestUtil.createJar(tempDir, "input.jar", "test/TestClass.class", "second.class");
    ClassArchive underTest = new ClassArchive(new JarFile(jar), transformed);

    // when
    File jarOut = new File(destination, "output.jar");
    try (JarOutputStream zout = jarOutputStream(jarOut)) {
      underTest.copyAllClassesTo(zout);
    }
    JarTestUtil.assertJar(
        destination,
        "output.jar",
        new String[] {"test/TestClass.class", "second.class"},
        new byte[][] {newTransformed, null});
  }

  private static JarOutputStream jarOutputStream(File destination) throws IOException {
    return new JarOutputStream(new FileOutputStream(destination));
  }
}
