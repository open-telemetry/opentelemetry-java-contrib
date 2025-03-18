/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestUtil {

  private TestUtil() {}

  public static ConfigProperties configProperties(Properties properties) {
    Map<String, String> map = new HashMap<>();
    properties.forEach((k, v) -> map.put((String) k, (String) v));
    return DefaultConfigProperties.createFromMap(map);
  }
}
