/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.assertJarContainsFiles;
import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnpackerTest extends AbstractTempDirTest {

  @Test
  void shouldUnpackNestedJars() throws Exception {
    // given
    Unpacker unpacker = new Unpacker(tempDir.toPath());
    PackagingSupport support = mock(PackagingSupport.class);
    Path jar = getResourcePath("test.jar");
    // when
    unpacker.copyAndExtract(jar, support);
    // then
    assertThat(Files.exists(tempDir.toPath().resolve("lib/firstNested.jar"))).isTrue();
    assertThat(Files.size(tempDir.toPath().resolve("lib/firstNested.jar"))).isGreaterThan(0);
    assertThat(Files.exists(tempDir.toPath().resolve("lib/secondNested.jar"))).isTrue();
    assertThat(Files.size(tempDir.toPath().resolve("lib/secondNested.jar"))).isGreaterThan(0);
  }

  @Test
  void shouldCopyTestJarContent() throws Exception {
    // given
    Path targetFolderPath = tempDir.toPath();
    Unpacker unpacker = new Unpacker(targetFolderPath);
    PackagingSupport support = PackagingSupport.EMPTY;
    Path jar = getResourcePath("test.jar");

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
