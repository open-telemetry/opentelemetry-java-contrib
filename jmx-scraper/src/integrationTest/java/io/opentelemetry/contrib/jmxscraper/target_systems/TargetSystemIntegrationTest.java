/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class TargetSystemIntegrationTest extends BaseTargetSystemIntegrationTest {

  @Test
  void endToEndTest(@TempDir Path tmpDir) {
    startContainers(tmpDir);
    verifyMetrics();
  }
}
