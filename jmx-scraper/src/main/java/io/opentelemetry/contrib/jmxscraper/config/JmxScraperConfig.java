/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ROOT;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** This class keeps application settings */
public final class JmxScraperConfig {

  private static final Logger logger = Logger.getLogger(JmxScraperConfig.class.getName());

  // metric sdk configuration
  static final String METRIC_EXPORT_INTERVAL = "otel.metric.export.interval";

  // not documented on purpose as using the SDK 'otel.metric.export.interval' is preferred
  static final String JMX_INTERVAL_LEGACY = "otel.jmx.interval.milliseconds";

  static final String JMX_SERVICE_URL = "otel.jmx.service.url";

  // the same as config option defined in instrumentation
  static final String JMX_CONFIG = "otel.jmx.config";

  // not documented on purpose as it's deprecated
  static final String JMX_CONFIG_LEGACY = "otel.jmx.custom.scraping.config";

  static final String JMX_TARGET_SYSTEM = "otel.jmx.target.system";
  static final String JMX_TARGET_SOURCE = "otel.jmx.target.source";

  static final String JMX_USERNAME = "otel.jmx.username";
  static final String JMX_PASSWORD = "otel.jmx.password";

  static final String JMX_REGISTRY_SSL = "otel.jmx.remote.registry.ssl";
  static final String JMX_REMOTE_PROFILE = "otel.jmx.remote.profile";
  static final String JMX_REALM = "otel.jmx.realm";

  private String serviceUrl = "";

  private List<String> jmxConfig = emptyList();

  private Set<String> targetSystems = emptySet();

  private TargetSystemSource targetSystemSource = TargetSystemSource.AUTO;

  private Duration samplingInterval = Duration.ofMinutes(1);

  @Nullable private String username;

  @Nullable private String password;

  @Nullable private String realm;

  @Nullable private String remoteProfile;
  private boolean registrySsl;

  public enum TargetSystemSource {
    AUTO,
    INSTRUMENTATION,
    LEGACY;

    static TargetSystemSource fromString(String source) {
      try {
        return TargetSystemSource.valueOf(source.toUpperCase(ROOT));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid target system source: " + source, e);
      }
    }
  }

  private JmxScraperConfig() {}

  public String getServiceUrl() {
    return serviceUrl;
  }

  public List<String> getJmxConfig() {
    return jmxConfig;
  }

  public Set<String> getTargetSystems() {
    return targetSystems;
  }

  /**
   * Get the list of target system rules.
   *
   * @param system system identifier
   * @return set of target system rules
   * @throws ConfigurationException when no matching rules are found
   */
  public Set<String> getTargetSystemRules(String system) {
    Set<String> set = new HashSet<>();
    switch (targetSystemSource) {
      case LEGACY:
        probeLegacy(system, set);
        break;
      case INSTRUMENTATION:
        probeInstrumentation(system, set);
        break;
      case AUTO:
        probeInstrumentation(system, set);
        if (set.isEmpty()) {
          probeLegacy(system, set);
        }
        break;
    }
    if (set.isEmpty()) {
      throw new ConfigurationException(
          "unsupported target system: '" + system + "', source: " + targetSystemSource);
    }

    return set;
  }

  private static void probeInstrumentation(String system, Set<String> set) {
    probeResource(String.format("jmx/rules/%s.yaml", system), set);
    probeResource(String.format("jmx/rules/%s_stable.yaml", system), set);
  }

  private static void probeLegacy(String system, Set<String> set) {
    probeResource(String.format("%s.yaml", system), set);
  }

  private static void probeResource(String path, Set<String> set) {
    try (InputStream resource = JmxScraperConfig.class.getClassLoader().getResourceAsStream(path)) {
      if (resource != null) {
        set.add(path);
      }
    } catch (IOException e) {
      throw new IllegalStateException("unable to probe resource: " + path, e);
    }
  }

  public Duration getSamplingInterval() {
    return samplingInterval;
  }

  @Nullable
  public String getUsername() {
    return username;
  }

  @Nullable
  public String getPassword() {
    return password;
  }

  @Nullable
  public String getRealm() {
    return realm;
  }

  @Nullable
  public String getRemoteProfile() {
    return remoteProfile;
  }

  public boolean isRegistrySsl() {
    return registrySsl;
  }

  /**
   * Builds JMX scraper configuration from auto-configuration
   *
   * @param config auto-configuration properties
   * @return JMX scraper configuration
   */
  public static JmxScraperConfig fromConfig(ConfigProperties config) {
    JmxScraperConfig scraperConfig = new JmxScraperConfig();

    Duration exportInterval = config.getDuration(METRIC_EXPORT_INTERVAL);
    if (exportInterval == null || exportInterval.isNegative() || exportInterval.isZero()) {
      // if not explicitly set, use default value of 1 minute as defined in specification
      scraperConfig.samplingInterval = Duration.ofMinutes(1);
    } else {
      scraperConfig.samplingInterval = exportInterval;
    }

    String serviceUrl = config.getString(JMX_SERVICE_URL);
    if (serviceUrl == null) {
      throw new ConfigurationException("missing mandatory " + JMX_SERVICE_URL);
    }
    scraperConfig.serviceUrl = serviceUrl;

    List<String> jmxConfig = config.getList(JMX_CONFIG);
    List<String> targetSystem = config.getList(JMX_TARGET_SYSTEM);

    // providing compatibility with the deprecated 'otel.jmx.custom.scraping.config' config option
    String jmxConfigDeprecated = config.getString(JMX_CONFIG_LEGACY);
    if (jmxConfigDeprecated != null) {
      logger.warning(
          JMX_CONFIG_LEGACY
              + " deprecated option is used, replacing with '"
              + JMX_CONFIG
              + "' is recommended");
      List<String> list = new ArrayList<>(jmxConfig);
      list.add(jmxConfigDeprecated);
      jmxConfig = list;
    }

    if (targetSystem.isEmpty() && jmxConfig.isEmpty()) {
      throw new ConfigurationException(
          "at least one of '" + JMX_TARGET_SYSTEM + "' or '" + JMX_CONFIG + "' must be set");
    }

    scraperConfig.jmxConfig = unmodifiableList(jmxConfig);
    scraperConfig.targetSystems = unmodifiableSet(new HashSet<>(targetSystem));

    scraperConfig.username = config.getString("otel.jmx.username");
    scraperConfig.password = config.getString("otel.jmx.password");
    scraperConfig.remoteProfile = config.getString("otel.jmx.remote.profile");
    scraperConfig.realm = config.getString("otel.jmx.realm");
    scraperConfig.registrySsl = config.getBoolean("otel.jmx.remote.registry.ssl", false);

    // checks target system is supported by resolving the yaml resources, throws exception on
    // missing/error
    scraperConfig.targetSystems.forEach(s -> scraperConfig.getTargetSystemRules(s));

    String source = config.getString(JMX_TARGET_SOURCE, TargetSystemSource.AUTO.name());
    scraperConfig.targetSystemSource = TargetSystemSource.fromString(source);

    return scraperConfig;
  }
}
