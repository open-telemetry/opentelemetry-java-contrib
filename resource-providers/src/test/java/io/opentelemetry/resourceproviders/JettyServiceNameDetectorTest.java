/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static io.opentelemetry.resourceproviders.JettyAppServer.parseJettyBase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JettyServiceNameDetectorTest {

  @Test
  void testJettyBase(@TempDir Path tempDir) throws IOException {
    assertNull(parseJettyBase(null));
    assertNull(parseJettyBase(""));
    assertNull(parseJettyBase("jetty.base="));
    assertEquals(tempDir.toString(), parseJettyBase("jetty.base=" + tempDir).toString());
    assertEquals(
        tempDir.toString(), parseJettyBase("foo jetty.base=" + tempDir + " bar").toString());

    Path otherDir = tempDir.resolve("jetty test");
    Files.createDirectory(otherDir);
    assertEquals(otherDir.toString(), parseJettyBase("jetty.base=" + otherDir).toString());
    assertEquals(
        otherDir.toString(), parseJettyBase("foo jetty.base=" + otherDir + " bar").toString());
  }
}
