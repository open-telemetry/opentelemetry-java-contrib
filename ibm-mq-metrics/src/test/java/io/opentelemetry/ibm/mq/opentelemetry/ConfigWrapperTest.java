/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

class ConfigWrapperTest {

  String file;

  @BeforeEach
  void setUp() {
    file = ConfigWrapperTest.class.getResource("/conf/config.yml").getFile();
    // Windows resources can contain a colon, which can't be mapped to a Path cleanly
    // They look like /D:/a/path/to/whatever
    file = file.replaceFirst("^/([A-Z]:)/", "$1/");
  }

  @Test
  void testQueueManagerNames() throws Exception {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getQueueManagerNames()).isEqualTo(singletonList("QM1"));
  }

  @Test
  void testNumberOfThreads() throws Exception {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getNumberOfThreads()).isEqualTo(20);
  }

  @Test
  void testTaskDelay() throws Exception {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getTaskDelay()).isEqualTo(Duration.of(27, ChronoUnit.SECONDS));
  }

  @Test
  void testTaskInitialDelay() throws Exception {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getTaskInitialDelaySeconds()).isEqualTo(0);
  }

  @Test
  void testYamlTagDeserializationRejected(@TempDir Path tempDir) throws IOException {
    Path tempFile = tempDir.resolve("gadget.yml");
    Files.write(tempFile, "!!java.lang.Runtime {}".getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> ConfigWrapper.parse(tempFile.toString()))
        .isInstanceOf(YamlEngineException.class);
  }
}
