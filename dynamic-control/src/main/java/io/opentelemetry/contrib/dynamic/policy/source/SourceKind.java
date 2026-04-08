/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import com.google.errorprone.annotations.Immutable;
import io.opentelemetry.contrib.dynamic.policy.OpampPolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceConfig;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Identifies where policy configuration is loaded from for registry initialization (e.g. local
 * file, OpAMP, HTTP). Distinct from {@link SourceFormat}, which describes how individual policy
 * lines or payloads are encoded (key-value vs JSON).
 */
public enum SourceKind {
  /** Policies loaded from a local file (e.g. line-per-policy file). */
  FILE("file", SourceKind::createNoProvider),

  /** Policies delivered via OpAMP (remote management). */
  OPAMP("opamp", SourceKind::createOpampProvider),

  /** Policies fetched from an HTTP/HTTPS endpoint. */
  HTTP("http", SourceKind::createNoProvider),

  /** User-defined or extension provider. */
  CUSTOM("custom", SourceKind::createNoProvider);

  private final String configValue;
  private final ProviderCreator providerCreator;
  private static final Logger logger = Logger.getLogger(SourceKind.class.getName());

  SourceKind(String configValue, ProviderCreator providerCreator) {
    this.configValue = configValue;
    this.providerCreator = providerCreator;
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
   * Creates a runtime {@link PolicyProvider} for this source kind.
   *
   * <p>Provider creation is delegated to a per-kind method reference configured on each enum
   * constant.
   */
  @Nullable
  public PolicyProvider createProvider(
      PolicySourceConfig source, ConfigProperties config, List<PolicyValidator> validators) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(config, "config cannot be null");
    Objects.requireNonNull(validators, "validators cannot be null");
    SourceKind sourceKind = source.getKind();
    if (sourceKind != this) {
      throw new IllegalArgumentException(
          "Source kind mismatch: expected " + this + " but was " + sourceKind);
    }
    return providerCreator.create(source, config, validators);
  }

  @Nullable
  private static PolicyProvider createNoProvider(
      PolicySourceConfig source, ConfigProperties config, List<PolicyValidator> validators) {
    return null;
  }

  @Nullable
  private static PolicyProvider createOpampProvider(
      PolicySourceConfig source, ConfigProperties config, List<PolicyValidator> validators) {
    String location = source.getLocation();
    if (location == null || location.trim().isEmpty()) {
      return null;
    }
    try {
      return new OpampPolicyProvider(
          config, location, source.getFormat(), source.getMappings(), validators);
    } catch (IllegalArgumentException e) {
      logger.log(
          Level.FINE,
          "Skipping OpAMP provider creation due to invalid/missing OpAMP configuration: {0}",
          e.getMessage());
      return null;
    }
  }

  @Immutable
  @FunctionalInterface
  private interface ProviderCreator {
    @Nullable
    PolicyProvider create(
        PolicySourceConfig source, ConfigProperties config, List<PolicyValidator> validators);
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
