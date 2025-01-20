/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class PropertiesSupplierTest {

  @Test
  void empty() {
    PropertiesSupplier supplier = new PropertiesSupplier(new Properties());
    assertThat(supplier.get()).isEmpty();
  }

  @Test
  void someValues() {
    Properties properties = new Properties();
    properties.setProperty("foo", "bar");
    properties.setProperty("hello", "world");
    PropertiesSupplier supplier = new PropertiesSupplier(properties);
    assertThat(supplier.get())
        .hasSize(2)
        .containsEntry("foo", "bar")
        .containsEntry("hello", "world");
  }
}
