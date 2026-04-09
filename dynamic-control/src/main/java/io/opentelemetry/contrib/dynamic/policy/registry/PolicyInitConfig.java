/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.contrib.dynamic.policy.registry.json.JsonPolicyInitConfigReader;
import io.opentelemetry.contrib.dynamic.policy.registry.yaml.YamlPolicyInitConfigReader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.IOException;
import java.io.InputStream;
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
