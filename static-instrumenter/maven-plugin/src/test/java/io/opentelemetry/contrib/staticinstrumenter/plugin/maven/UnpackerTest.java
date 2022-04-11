/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UnpackerTest {

  @Test
  void shouldUnpackNestedJars(@TempDir File targetFolder) throws Exception {
    // given
    Unpacker unpacker = new Unpacker(targetFolder.toPath());
    PackagingSupport support = mock(PackagingSupport.class);
    Path jar = Paths.get(getResourcePath("test.jar"));
    // when
    unpacker.copyAndUnpack(jar, support);
    // then
    assertThat(Files.exists(targetFolder.toPath().resolve("lib/firstNested.jar"))).isTrue();
    assertThat(Files.size(targetFolder.toPath().resolve("lib/firstNested.jar"))).isGreaterThan(0);
    assertThat(Files.exists(targetFolder.toPath().resolve("lib/secondNested.jar"))).isTrue();
    assertThat(Files.size(targetFolder.toPath().resolve("lib/secondNested.jar"))).isGreaterThan(0);
  }

  @Test
  void shouldCopyTestJarContent(@TempDir File targetFolder) throws Exception {
    // given
    Unpacker unpacker = new Unpacker(targetFolder.toPath());
    PackagingSupport support = mock(PackagingSupport.class);
    Path jar = Paths.get(getResourcePath("test.jar"));
    ArgumentCaptor<JarFile> captor = ArgumentCaptor.forClass(JarFile.class);
    // when
    unpacker.copyAndUnpack(jar, support);
    // then
    then(support).should().copyRemovingPrefix(captor.capture(), Mockito.any(ZipOutputStream.class));
    JarFile captured = captor.getValue();
    // copied the right file?
    assertThat(captured.getName()).isEqualTo(jar.toString());
    // got the target file right?
    assertThat(Files.exists(targetFolder.toPath().resolve("test.jar"))).isTrue();
  }
}
