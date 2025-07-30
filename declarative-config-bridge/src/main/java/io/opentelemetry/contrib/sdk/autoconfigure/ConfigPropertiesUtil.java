/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sdk.autoconfigure;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ConfigPropertiesUtil {
  private ConfigPropertiesUtil() {}

  public static String propertyYamlPath(String propertyName) {
    return DeclarativeConfigPropertiesBridge.yamlPath(propertyName);
  }
}
