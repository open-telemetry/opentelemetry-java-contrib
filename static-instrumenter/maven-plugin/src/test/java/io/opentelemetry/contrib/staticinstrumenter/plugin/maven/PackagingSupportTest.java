/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJar;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class PackagingSupportTest {

  @Test
  void shouldCopyRemovingAndAddingPrefix() throws Exception {
    // given
    Path path = Paths.get(getResourcePath("spring-boot.jar"));
    PackagingSupport underTest = PackagingSupportFactory.packagingSupportFor(path);
    // when
    // copy files to new jar removing prefix
    Path withoutPrefix =
        JarTestUtil.createJar(
            "without-prefix",
            (target) -> {
              underTest.copyRemovingPrefix(new JarFile(path.toFile()), target);
            });
    // copy files back, adding prefix
    JarFile withoutPrefixJar = new JarFile(withoutPrefix.toFile());
    Path clone =
        JarTestUtil.createJar(
            "clone",
            (target) -> {
              JarSupport.consumeEntries(
                  withoutPrefixJar,
                  (entry) -> underTest.copyAddingPrefix(entry, withoutPrefixJar, target));
            });
    // then
    assertJar(
        withoutPrefix,
        "META-INF/MANIFEST.MF",
        "first.class",
        "com/test/second.class",
        "BOOT-INF/lib/test.jar");
    assertJar(
        clone,
        "META-INF/MANIFEST.MF",
        "BOOT-INF/classes/first.class",
        "BOOT-INF/classes/com/test/second.class",
        "BOOT-INF/lib/test.jar");
  }
}
