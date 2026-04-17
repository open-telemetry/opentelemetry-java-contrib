/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.ExtendedOpenTelemetry;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.registry.json.JsonPolicyInitConfigReader;
import io.opentelemetry.contrib.dynamic.policy.registry.yaml.YamlPolicyInitConfigReader;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Top-level registry initialization model containing per-source mapping config. */
public final class PolicyInitConfig {
  static final String TELEMETRY_POLICY_DECLARATIVE_KEY = "telemetry_policy";
  static final String SOURCES_DECLARATIVE_KEY = "sources";
  static final String KIND_DECLARATIVE_KEY = "kind";
  static final String FORMAT_DECLARATIVE_KEY = "format";
  static final String LOCATION_DECLARATIVE_KEY = "location";
  static final String MAPPINGS_DECLARATIVE_KEY = "mappings";
  static final String SOURCE_KEY_DECLARATIVE_KEY = "sourceKey";
  static final String POLICY_TYPE_DECLARATIVE_KEY = "policyType";
  static final String POLICY_INIT_CONFIG_PROPERTY_JSON =
      "otel.java.experimental.telemetry.policy.init.json";
  static final String POLICY_INIT_CONFIG_PROPERTY_YAML =
      "otel.java.experimental.telemetry.policy.init.yaml";
  private static final Logger logger = Logger.getLogger(PolicyInitConfig.class.getName());

  private final List<PolicySourceConfig> sources;

  public PolicyInitConfig(List<PolicySourceConfig> sources) {
    List<PolicySourceConfig> sourceCopy =
        new ArrayList<>(Objects.requireNonNull(sources, "sources cannot be null"));
    for (PolicySourceConfig source : sourceCopy) {
      Objects.requireNonNull(source, "sources cannot contain null elements");
    }
    this.sources = Collections.unmodifiableList(sourceCopy);
  }

  public List<PolicySourceConfig> getSources() {
    return sources;
  }

  /**
   * Reads policy-init configuration from declarative config properties.
   *
   * <p>Expected shape is:
   *
   * <pre>{@code
   * telemetry_policy:
   *   sources:
   *     - kind: ...
   *       format: ...
   *       location: ...
   *       mappings:
   *         - sourceKey: ...
   *           policyType: ...
   * }</pre>
   *
   * @param declarativeConfig declarative config root
   * @return parsed init config, or null when telemetry_policy/sources is not configured
   * @throws NullPointerException if declarativeConfig is null
   * @throws IllegalArgumentException if telemetry_policy is present but invalid
   */
  @Nullable
  public static PolicyInitConfig readFromDeclarativeConfigProperties(
      DeclarativeConfigProperties declarativeConfig) {
    Objects.requireNonNull(declarativeConfig, "declarativeConfig cannot be null");
    DeclarativeConfigProperties telemetryPolicyConfig =
        declarativeConfig.getStructured(TELEMETRY_POLICY_DECLARATIVE_KEY);
    if (telemetryPolicyConfig == null) {
      return null;
    }
    List<DeclarativeConfigProperties> sourceConfigs =
        telemetryPolicyConfig.getStructuredList(SOURCES_DECLARATIVE_KEY);
    if (sourceConfigs == null || sourceConfigs.isEmpty()) {
      return null;
    }

    List<PolicySourceConfig> sources = new ArrayList<>();
    for (DeclarativeConfigProperties sourceConfig : sourceConfigs) {
      sources.add(parseDeclarativeSource(sourceConfig));
    }
    return new PolicyInitConfig(sources);
  }

  /**
   * Reads policy-init configuration with declarative-first fallback.
   *
   * <p>When declarative config contains {@code telemetry_policy.sources}, that configuration is
   * used. Otherwise, this falls back to file-path based loading via {@link
   * #readFromConfigProperties(ConfigProperties)}.
   */
  @Nullable
  public static PolicyInitConfig readFromDeclarativeOrConfigProperties(
      ConfigProperties config, DeclarativeConfigProperties declarativeConfig) {
    Objects.requireNonNull(config, "config cannot be null");
    Objects.requireNonNull(declarativeConfig, "declarativeConfig cannot be null");
    PolicyInitConfig fromDeclarative = readFromDeclarativeConfigProperties(declarativeConfig);
    if (fromDeclarative != null) {
      return fromDeclarative;
    }
    return readFromConfigProperties(config);
  }

  /**
   * Reads policy-init configuration with declarative-first fallback via {@link OpenTelemetry}.
   *
   * <p>If {@code openTelemetry} is an {@link ExtendedOpenTelemetry}, this method reads {@link
   * ConfigProvider} general declarative config and tries to parse {@code telemetry_policy} from
   * there. If unavailable, this falls back to file-path loading via {@link
   * #readFromConfigProperties(ConfigProperties)}.
   */
  @Nullable
  public static PolicyInitConfig readFromOpenTelemetryOrConfigProperties(
      ConfigProperties config, @Nullable OpenTelemetry openTelemetry) {
    Objects.requireNonNull(config, "config cannot be null");
    ConfigProvider configProvider = getConfigProvider(openTelemetry);
    if (configProvider != null) {
      PolicyInitConfig fromDeclarative =
          readFromDeclarativeConfigProperties(getGeneralDeclarativeConfig(configProvider));
      if (fromDeclarative != null) {
        return fromDeclarative;
      }
    }
    return readFromConfigProperties(config);
  }

  /**
   * Reads policy-init configuration based on config properties.
   *
   * <p>YAML takes precedence over JSON when both are present. If both are present, and the YAML
   * file is invalid, the JSON file is still ignored. If the file parsed is invalid, a warning is
   * logged and null is returned.
   *
   * @param config OpenTelemetry config properties
   * @return parsed init config, or null when no init-config path is configured or the file is
   *     invalid
   * @throws NullPointerException if config is null
   */
  @Nullable
  public static PolicyInitConfig readFromConfigProperties(ConfigProperties config) {
    Objects.requireNonNull(config, "config cannot be null");
    String mappingPathYaml = config.getString(POLICY_INIT_CONFIG_PROPERTY_YAML);
    if (mappingPathYaml == null || mappingPathYaml.trim().isEmpty()) {
      String mappingPathJson = config.getString(POLICY_INIT_CONFIG_PROPERTY_JSON);
      if (mappingPathJson == null || mappingPathJson.trim().isEmpty()) {
        return null;
      } else {
        try (InputStream in = Files.newInputStream(Paths.get(mappingPathJson.trim()))) {
          return JsonPolicyInitConfigReader.read(in);
        } catch (IOException | RuntimeException e) {
          logger.log(
              Level.WARNING,
              "Failed to load telemetry policy init config from {0}",
              mappingPathJson.trim());
          logger.log(Level.WARNING, "Policy init config read failed", e);
          return null;
        }
      }
    } else {
      try (InputStream in = Files.newInputStream(Paths.get(mappingPathYaml.trim()))) {
        return YamlPolicyInitConfigReader.read(in);
      } catch (IOException | RuntimeException e) {
        logger.log(
            Level.WARNING,
            "Failed to load telemetry policy init config from {0}",
            mappingPathYaml.trim());
        logger.log(Level.WARNING, "Policy init config read failed", e);
        return null;
      }
    }
  }

  private static PolicySourceConfig parseDeclarativeSource(
      DeclarativeConfigProperties sourceConfig) {
    Objects.requireNonNull(sourceConfig, "source config cannot be null");
    String kindValue =
        requireDeclarativeText(
            sourceConfig.getString(KIND_DECLARATIVE_KEY), "Each source must define string 'kind'.");
    String formatValue =
        requireDeclarativeText(
            sourceConfig.getString(FORMAT_DECLARATIVE_KEY),
            "Each source must define string 'format'.");
    String location = sourceConfig.getString(LOCATION_DECLARATIVE_KEY);

    List<DeclarativeConfigProperties> mappingConfigs =
        sourceConfig.getStructuredList(MAPPINGS_DECLARATIVE_KEY);
    if (mappingConfigs == null) {
      throw new IllegalArgumentException("Each source must define a 'mappings' array.");
    }
    List<PolicySourceMappingConfig> mappings = new ArrayList<>();
    for (DeclarativeConfigProperties mappingConfig : mappingConfigs) {
      mappings.add(parseDeclarativeMapping(mappingConfig));
    }

    return new PolicySourceConfig(
        SourceKind.fromConfigValue(kindValue),
        SourceFormat.fromConfigValue(formatValue),
        location,
        mappings);
  }

  private static PolicySourceMappingConfig parseDeclarativeMapping(
      DeclarativeConfigProperties mappingConfig) {
    Objects.requireNonNull(mappingConfig, "mapping config cannot be null");
    String sourceKey =
        requireDeclarativeText(
            mappingConfig.getString(SOURCE_KEY_DECLARATIVE_KEY),
            "Each mapping must define string 'sourceKey'.");
    String policyType =
        requireDeclarativeText(
            mappingConfig.getString(POLICY_TYPE_DECLARATIVE_KEY),
            "Each mapping must define string 'policyType'.");
    return new PolicySourceMappingConfig(sourceKey, policyType);
  }

  private static String requireDeclarativeText(@Nullable String value, String message) {
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return trimmed;
  }

  @Nullable
  static ConfigProvider getConfigProvider(@Nullable OpenTelemetry openTelemetry) {
    return openTelemetry instanceof ExtendedOpenTelemetry
        ? ((ExtendedOpenTelemetry) openTelemetry).getConfigProvider()
        : null;
  }

  private static DeclarativeConfigProperties getGeneralDeclarativeConfig(
      ConfigProvider configProvider) {
    // Prefer the general config accessor when available in the API/runtime.
    try {
      Method method = configProvider.getClass().getMethod("getGeneralConfig");
      Object maybeConfig = method.invoke(configProvider);
      if (maybeConfig instanceof DeclarativeConfigProperties) {
        return (DeclarativeConfigProperties) maybeConfig;
      }
    } catch (NoSuchMethodException
        | IllegalAccessException
        | InvocationTargetException
        | RuntimeException ignored) {
      // Fall through to backwards-compatible accessor below.
    }
    return configProvider.getGeneralInstrumentationConfig();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PolicyInitConfig)) {
      return false;
    }
    PolicyInitConfig that = (PolicyInitConfig) obj;
    return sources.equals(that.sources);
  }

  @Override
  public int hashCode() {
    return sources.hashCode();
  }
}
