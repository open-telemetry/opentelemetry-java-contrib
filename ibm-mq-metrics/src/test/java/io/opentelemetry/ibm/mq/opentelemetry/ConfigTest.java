/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
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
    Config.setUpSslConnection(
        new HashMap<String, Object>(
            ImmutableMap.of(
                "keyStorePath", "foo",
                "trustStorePath", "bar",
                "keyStorePassword", "password",
                "trustStorePassword", "password1")));

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
