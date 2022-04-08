/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.plugin.maven;

import static io.opentelemetry.contrib.staticinstrumenter.plugin.maven.JarTestUtil.getResourcePath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class PackagingSupportFactoryTest {

  @Test
  void shouldReturnForSpringBoot() throws Exception {
    // given
    Path path = Paths.get(getResourcePath("spring-boot.jar"));
    // when
    PackagingSupport result = PackagingSupportFactory.packagingSupportFor(path);
    // then
    assertThat(result).isNotEqualTo(PackagingSupport.EMPTY);
    assertThat(result.getClassesPrefix()).isEqualTo("BOOT-INF/classes/");
  }

  @Test
  void shouldReturnForWar() throws Exception {
    // given
    Path path = Paths.get(getResourcePath("web.war"));
    // when
    PackagingSupport result = PackagingSupportFactory.packagingSupportFor(path);
    // then
    assertThat(result).isNotEqualTo(PackagingSupport.EMPTY);
    assertThat(result.getClassesPrefix()).isEqualTo("WEB-INF/classes/");
  }

  @Test
  void shouldReturnEmptyForUnsupported() throws Exception {
    // given
    Path path = Paths.get(getResourcePath("test.jar"));
    // when
    PackagingSupport result = PackagingSupportFactory.packagingSupportFor(path);
    // then
    assertThat(result).isEqualTo(PackagingSupport.EMPTY);
  }
}
