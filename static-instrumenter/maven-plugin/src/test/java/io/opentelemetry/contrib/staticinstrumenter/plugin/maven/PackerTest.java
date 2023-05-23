/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PackerTest extends AbstractTempDirTest {

  @Test
  void shouldCopyClassesAddingPrefix() throws Exception {
    // given
    Path jar = getResourcePath("test.jar");
    Packer packer = new Packer(tempDir.toPath(), "-processed");
    PackagingSupport support = Mockito.mock(PackagingSupport.class);
    ArgumentCaptor<JarFile> captor = ArgumentCaptor.forClass(JarFile.class);
    // when
    Path copy = packer.packAndCopy(jar, support);
    // then
    assertThat(copy.getFileName().toString()).isEqualTo("test-processed.jar");
    then(support)
        .should(Mockito.times(6))
        .copyAddingPrefix(any(JarEntry.class), captor.capture(), any(ZipOutputStream.class));
    JarFile source = captor.getValue();
    assertThat(source.getName()).isEqualTo(getResourcePath("test.jar").toString());
    assertThat(Files.exists(copy)).isTrue();
  }

  @Test
  void shouldPackNestedJarsToCopiedArtifact() throws Exception {
    // given
    Path jar = getResourcePath("test.jar");
    Packer packer = new Packer(tempDir.toPath(), "-processed");
    PackagingSupport support = Mockito.mock(PackagingSupport.class);
    ArgumentCaptor<JarFile> captor = ArgumentCaptor.forClass(JarFile.class);
    // when
    Path copy = packer.packAndCopy(jar, support);
    // then
    JarFile copyJar = new JarFile(copy.toFile());
    verifyEntry("lib/firstNested.jar", copyJar);
    verifyEntry("lib/secondNested.jar", copyJar);

    assertThat(copy.getFileName().toString()).isEqualTo("test-processed.jar");
    then(support)
        .should(Mockito.times(6))
        .copyAddingPrefix(any(JarEntry.class), captor.capture(), any(ZipOutputStream.class));
    JarFile source = captor.getValue();
    assertThat(source.getName()).isEqualTo(getResourcePath("test.jar").toString());
    assertThat(Files.exists(copy)).isTrue();
  }

  private static void verifyEntry(String name, JarFile jar) throws Exception {
    JarEntry firstEntry = jar.getJarEntry(name);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    jar.getInputStream(firstEntry).transferTo(baos);
    assertThat(baos.toString(Charset.defaultCharset())).isEqualTo("not a real jar file\n");
  }
}
