/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJarContainsFiles;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnpackerTest {

  @Test
  void shouldUnpackNestedJars(@TempDir File targetFolder) throws Exception {
    // given
    Unpacker unpacker = new Unpacker(targetFolder.toPath());
    PackagingSupport support = mock(PackagingSupport.class);
    Path jar = Paths.get(getResourcePath("test.jar"));
    // when
    unpacker.copyAndExtract(jar, support);
    // then
    assertThat(Files.exists(targetFolder.toPath().resolve("lib/firstNested.jar"))).isTrue();
    assertThat(Files.size(targetFolder.toPath().resolve("lib/firstNested.jar"))).isGreaterThan(0);
    assertThat(Files.exists(targetFolder.toPath().resolve("lib/secondNested.jar"))).isTrue();
    assertThat(Files.size(targetFolder.toPath().resolve("lib/secondNested.jar"))).isGreaterThan(0);
  }

  @Test
  void shouldCopyTestJarContent(@TempDir File targetFolder) throws Exception {
    // given
    Path targetFolderPath = targetFolder.toPath();
    Unpacker unpacker = new Unpacker(targetFolderPath);
    PackagingSupport support = PackagingSupport.EMPTY;
    Path jar = Paths.get(getResourcePath("test.jar"));

    // when
    List<Path> copied = unpacker.copyAndExtract(jar, support);
    // then
    assertThat(copied).hasSize(3);
    // copied the right file?
    assertThat(copied)
        .containsExactlyInAnyOrder(
            targetFolderPath.resolve("test.jar"),
            targetFolderPath.resolve("lib/firstNested.jar"),
            targetFolderPath.resolve("lib/secondNested.jar"));
    // got the target file right?
    assertJarContainsFiles(
        copied.get(0),
        "META-INF/MANIFEST.MF",
        "test/NotInstrumented.class",
        "test/TestClass.class",
        "lib/firstNested.jar",
        "lib/secondNested.jar");
  }
}
