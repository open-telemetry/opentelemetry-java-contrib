/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Configuration context shared by policy providers. */
public final class PolicyProviderConfig {
  private final DeclarativeConfigProperties properties;
  private final Map<String, String> resourceAttributes;
  private final Map<String, String> opampHeaders;

  /** Creates a configuration context without legacy map properties. */
  public static PolicyProviderConfig create(DeclarativeConfigProperties properties) {
    return new PolicyProviderConfig(properties, Collections.emptyMap(), Collections.emptyMap());
  }

  /**
   * Creates a configuration context with declarative properties and legacy map properties.
   *
   * <p>The maps are kept separately because the flat-to-declarative bridge cannot expose legacy map
   * properties.
   */
  public static PolicyProviderConfig createWithLegacyProperties(
      DeclarativeConfigProperties properties,
      Map<String, String> resourceAttributes,
      Map<String, String> opampHeaders) {
    return new PolicyProviderConfig(properties, resourceAttributes, opampHeaders);
  }

  private PolicyProviderConfig(
      DeclarativeConfigProperties properties,
      Map<String, String> resourceAttributes,
      Map<String, String> opampHeaders) {
    this.properties = Objects.requireNonNull(properties, "properties cannot be null");
    this.resourceAttributes = copyMap(resourceAttributes, "resourceAttributes");
    this.opampHeaders = copyMap(opampHeaders, "opampHeaders");
  }

  private static Map<String, String> copyMap(Map<String, String> values, String propertyName) {
    return Collections.unmodifiableMap(
        new HashMap<>(Objects.requireNonNull(values, propertyName + " cannot be null")));
  }

  public DeclarativeConfigProperties getProperties() {
    return properties;
  }

  public Map<String, String> getResourceAttributes() {
    return resourceAttributes;
  }

  public Map<String, String> getOpampHeaders() {
    return opampHeaders;
  }
}
