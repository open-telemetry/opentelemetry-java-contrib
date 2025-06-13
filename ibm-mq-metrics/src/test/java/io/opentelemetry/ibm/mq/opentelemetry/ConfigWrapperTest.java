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
package io.opentelemetry.ibm.mq.opentelemetry;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigWrapperTest {

  String file;

  @BeforeEach
  void setUp() {
    file = ConfigWrapperTest.class.getResource("/conf/config.yml").getFile();
  }

  @Test
  void testQueueManagerNames() throws FileNotFoundException {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getQueueManagerNames()).isEqualTo(singletonList("QM1"));
  }

  @Test
  void testNumberOfThreads() throws FileNotFoundException {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getNumberOfThreads()).isEqualTo(20);
  }

  @Test
  void testTaskDelay() throws FileNotFoundException {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getTaskDelay()).isEqualTo(Duration.of(27, ChronoUnit.SECONDS));
  }

  @Test
  void testTaskInitialDelay() throws FileNotFoundException {
    ConfigWrapper config = ConfigWrapper.parse(file);
    assertThat(config.getTaskInitialDelaySeconds()).isEqualTo(0);
  }
}
