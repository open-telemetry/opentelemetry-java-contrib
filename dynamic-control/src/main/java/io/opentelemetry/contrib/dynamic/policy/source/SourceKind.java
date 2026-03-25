/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifies where policy configuration is loaded from for registry initialization (e.g. local
 * file, OpAMP, HTTP). Distinct from {@link SourceFormat}, which describes how individual policy
 * lines or payloads are encoded (key-value vs JSON).
 */
public enum SourceKind {
  /** Policies loaded from a local file (e.g. line-per-policy file). */
  FILE("file"),

  /** Policies delivered via OpAMP (remote management). */
  OPAMP("opamp"),

  /** Policies fetched from an HTTP/HTTPS endpoint. */
  HTTP("http"),

  /** User-defined or extension provider. */
  CUSTOM("custom");

  private final String configValue;

  SourceKind(String configValue) {
    this.configValue = configValue;
  }

  /**
   * Stable string used in registry JSON configuration (lowercase).
   *
   * @return the config value for this kind
   */
  public String configValue() {
    return configValue;
  }

  /**
   * Parses the value used in JSON configuration. Leading and trailing whitespace is removed, then
   * the remainder is matched case-insensitively against {@link #configValue()} for each kind.
   *
   * @param value the string from config (e.g. {@code "file"}, {@code "OPAMP"})
   * @return the matching kind
   * @throws NullPointerException if value is null
   * @throws IllegalArgumentException if no kind matches the trimmed value
   */
  public static SourceKind fromConfigValue(String value) {
    Objects.requireNonNull(value, "value cannot be null");
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    for (SourceKind kind : values()) {
      if (kind.configValue.equals(normalized)) {
        return kind;
      }
    }
    throw new IllegalArgumentException("Unknown source kind: " + value);
  }
}
