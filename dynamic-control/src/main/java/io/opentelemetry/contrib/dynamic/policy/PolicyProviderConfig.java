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
  private final Map<String, String> opampHeaders;

  /** Creates a configuration context without legacy OpAMP headers. */
  public static PolicyProviderConfig create(DeclarativeConfigProperties properties) {
    return new PolicyProviderConfig(properties, Collections.emptyMap());
  }

  /**
   * Creates a configuration context with declarative properties and legacy OpAMP headers.
   *
   * <p>The headers are kept separately because the flat-to-declarative bridge cannot enumerate keys
   * from system-property-backed maps.
   */
  public static PolicyProviderConfig createWithOpampHeaders(
      DeclarativeConfigProperties properties, Map<String, String> opampHeaders) {
    return new PolicyProviderConfig(properties, opampHeaders);
  }

  private PolicyProviderConfig(
      DeclarativeConfigProperties properties, Map<String, String> opampHeaders) {
    this.properties = Objects.requireNonNull(properties, "properties cannot be null");
    this.opampHeaders =
        Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(opampHeaders, "opampHeaders cannot be null")));
  }

  public DeclarativeConfigProperties getProperties() {
    return properties;
  }

  public Map<String, String> getOpampHeaders() {
    return opampHeaders;
  }
}
