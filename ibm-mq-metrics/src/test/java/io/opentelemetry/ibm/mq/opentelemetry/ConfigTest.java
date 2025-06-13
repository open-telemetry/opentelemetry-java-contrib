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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigTest {

  private Properties systemProperties;

  @BeforeEach
  public void cacheSystemProperties() {
    systemProperties = new Properties();
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      systemProperties.put(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  @Test
  void testSSLConnection() {
    Config.setUpSSLConnection(
        new HashMap<String, Object>() {
          {
            put("keyStorePath", "foo");
            put("trustStorePath", "bar");
            put("keyStorePassword", "password");
            put("trustStorePassword", "password1");
          }
        });

    assertThat(System.getProperties().get("javax.net.ssl.keyStore")).isEqualTo("foo");
    assertThat(System.getProperties().get("javax.net.ssl.trustStorePath")).isEqualTo("bar");
    assertThat(System.getProperties().get("javax.net.ssl.keyStorePassword")).isEqualTo("password");
    assertThat(System.getProperties().get("javax.net.ssl.trustStorePassword"))
        .isEqualTo("password1");
  }

  @AfterEach
  public void resetSystemProperties() {
    System.getProperties().clear();
    for (Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
      System.setProperty(entry.getKey().toString(), entry.getValue().toString());
    }
  }
}
