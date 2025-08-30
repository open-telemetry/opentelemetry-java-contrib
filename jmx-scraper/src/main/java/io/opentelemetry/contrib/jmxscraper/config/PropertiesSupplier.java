/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/** Configuration supplier for java properties */
public class PropertiesSupplier implements Supplier<Map<String, String>> {

  private final Properties properties;

  public PropertiesSupplier(Properties properties) {
    this.properties = properties;
  }

  @Override
  public Map<String, String> get() {
    Map<String, String> map = new HashMap<>();
    properties.forEach((k, v) -> map.put((String) k, (String) v));
    return map;
  }
}
