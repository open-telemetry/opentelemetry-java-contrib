/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJarContainsFiles;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.createJar;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class PackagingSupportTest {

  @Test
  void shouldCopyPreservingPrefix() throws Exception {
    // given
    Path path = Paths.get(getResourcePath("test.jar"));
    PackagingSupport underTest = PackagingSupportFactory.packagingSupportFor(path);
    // when
    Path withoutPrefix =
        createJar(
            "copied-jar",
            (target) -> {
              underTest.copyRemovingPrefix(new JarFile(path.toFile()), target);
            });
    // then
    JarTestUtil.assertJarContainsFiles(
        withoutPrefix,
        "META-INF/MANIFEST.MF",
        "test/NotInstrumented.class",
        "test/TestClass.class",
        "lib/firstNested.jar",
        "lib/secondNested.jar");
  }

  @Test
  void shouldCopyRemovingAndAddingPrefix() throws Exception {
    // given
    Path path = Paths.get(getResourcePath("spring-boot.jar"));
    PackagingSupport underTest = PackagingSupportFactory.packagingSupportFor(path);
    // when
    // copy files to new jar removing prefix
    Path withoutPrefix =
        createJar(
            "without-prefix",
            (target) -> {
              underTest.copyRemovingPrefix(new JarFile(path.toFile()), target);
            });
    // copy files back, adding prefix
    JarFile withoutPrefixJar = new JarFile(withoutPrefix.toFile());
    Path clone =
        createJar(
            "clone",
            (target) -> {
              JarSupport.consumeEntries(
                  withoutPrefixJar,
                  (entry) -> underTest.copyAddingPrefix(entry, withoutPrefixJar, target));
            });
    // then
    assertJarContainsFiles(
        withoutPrefix,
        "META-INF/MANIFEST.MF",
        "first.class",
        "com/test/second.class",
        "BOOT-INF/lib/test.jar");
    assertJarContainsFiles(
        clone,
        "META-INF/MANIFEST.MF",
        "BOOT-INF/classes/first.class",
        "BOOT-INF/classes/com/test/second.class",
        "BOOT-INF/lib/test.jar");
  }
}
