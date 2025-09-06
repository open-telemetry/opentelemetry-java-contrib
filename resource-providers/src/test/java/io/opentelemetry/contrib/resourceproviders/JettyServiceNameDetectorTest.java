/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static io.opentelemetry.contrib.resourceproviders.JettyAppServer.parseJettyBase;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JettyServiceNameDetectorTest {

  @Test
  void testJettyBase(@TempDir Path tempDir) throws IOException {
    assertThat(parseJettyBase(null)).isNull();
    assertThat(parseJettyBase("")).isNull();
    assertThat(parseJettyBase("jetty.base=")).isNull();
    assertThat(parseJettyBase("jetty.base=" + tempDir).toString()).isEqualTo(tempDir.toString());
    assertThat(parseJettyBase("foo jetty.base=" + tempDir + " bar").toString())
        .isEqualTo(tempDir.toString());

    Path otherDir = tempDir.resolve("jetty test");
    Files.createDirectory(otherDir);
    assertThat(parseJettyBase("jetty.base=" + otherDir).toString()).isEqualTo(otherDir.toString());
    assertThat(parseJettyBase("foo jetty.base=" + otherDir + " bar").toString())
        .isEqualTo(otherDir.toString());
  }
}
