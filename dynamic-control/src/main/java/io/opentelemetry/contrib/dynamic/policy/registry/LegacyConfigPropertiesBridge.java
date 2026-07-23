/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Extracts map-shaped legacy properties that the flat-to-declarative bridge cannot expose. */
final class LegacyConfigPropertiesBridge {
  private static final String OPAMP_HEADERS = "otel.experimental.opamp.headers";
  private static final String RESOURCE_ATTRIBUTES = "otel.resource.attributes";

  private LegacyConfigPropertiesBridge() {}

  static Map<String, String> getResourceAttributes(ConfigProperties configProperties) {
    return copyMap(configProperties.getMap(RESOURCE_ATTRIBUTES));
  }

  static Map<String, String> getOpampHeaders(ConfigProperties configProperties) {
    return copyMap(configProperties.getMap(OPAMP_HEADERS));
  }

  private static Map<String, String> copyMap(Map<String, String> values) {
    return values.isEmpty()
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(new HashMap<>(values));
  }
}
