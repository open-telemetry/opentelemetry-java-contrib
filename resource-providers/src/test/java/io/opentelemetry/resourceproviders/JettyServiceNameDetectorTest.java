/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
