/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigWrapperTest {

  String file;

  @BeforeEach
  @SuppressWarnings("SystemOut")
  void setUp() {
    file = ConfigWrapperTest.class.getResource("/conf/config.yml").getFile();
    // Windows resources can contain a colon, which can't be mapped to a Path cleanly
    if (file.contains(":")) {
      System.err.println("resource => " + ConfigWrapperTest.class.getResource("/conf/config.yml"));
      System.err.println("config file => " + file);
      String file2 = file.replaceFirst("^/([A-Z]:)/", "$1/");
      System.err.println("file2 => " + file2);
      String userDir = System.getProperty("user.dir");
      System.err.println("user.dir => " + userDir);
      file = file2;
    }
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
}
