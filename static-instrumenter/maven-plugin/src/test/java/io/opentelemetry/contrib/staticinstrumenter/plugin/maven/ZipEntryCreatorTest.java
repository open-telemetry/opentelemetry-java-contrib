/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJar;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.createJar;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.ZipEntryCreator.createZipEntryFromFile;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.ZipEntryCreator.moveEntry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class ZipEntryCreatorTest {

  @Test
  void shouldMoveEntryWithoutRenaming() throws Exception {
    // given
    JarFile source = new JarFile(getResourcePath("test.jar"));
    // when
    Path targetFile =
        createJar(
            "test-copy",
            (target) ->
                moveEntry(
                    target,
                    "test/TestClass.class",
                    source.getJarEntry("test/TestClass.class"),
                    source));
    // then
    assertJar(targetFile, "test/TestClass.class");
  }

  @Test
  void shouldMoveEntryRenaming() throws Exception {
    // given
    JarFile source = new JarFile(getResourcePath("test.jar"));
    // when
    Path targetFile =
        createJar(
            "test-copy",
            (target) -> {
              moveEntry(
                  target,
                  "newpath/TestClassRenamed.classdata",
                  source.getJarEntry("test/TestClass.class"),
                  source);
              moveEntry(
                  target,
                  "test/NotInstrumented.class",
                  source.getJarEntry("test/NotInstrumented.class"),
                  source);
            });
    // then
    assertJar(targetFile, "newpath/TestClassRenamed.classdata", "test/NotInstrumented.class");
  }

  @Test
  void shouldAddFileToJar() throws Exception {
    // given
    Path file = Paths.get(getResourcePath("testing.file"));
    // when
    Path targetFile =
        createJar("new-jar", (target) -> createZipEntryFromFile(target, file, "stored/entry.file"));
    // then
    assertJar(targetFile, "stored/entry.file");
  }
}
