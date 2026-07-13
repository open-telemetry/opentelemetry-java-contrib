/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Declarative sampler component that bootstraps top-level telemetry policy wiring. */
@AutoService(ComponentProvider.class)
public final class TelemetryPolicySamplerComponentProvider implements ComponentProvider {
  private static final Logger logger =
      Logger.getLogger(TelemetryPolicySamplerComponentProvider.class.getName());
  public static final String NAME = "telemetry_policy/development";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Sampler create(DeclarativeConfigProperties config) {
    logger.log(
        Level.INFO,
        "Dynamic control extension has been loaded by the agent via declarative config");
    ConfigProperties bridgedConfig =
        new SystemPropertyFallbackConfigProperties(
            new DeclarativeConfigPropertiesBridgeBuilder().build(config));
    try {
      PolicyInit.initFromDeclarativeConfig(config, bridgedConfig);
    } catch (IllegalArgumentException e) {
      logger.log(Level.WARNING, "Failed to initialize telemetry policy from component config", e);
    }
    // TODO: install specifically a delegating sampler, and allow it to be dynamically updated by
    // the policy configuration
    // but for now just use the existing sampling rate sampler which is a specifically configured
    // delegating sampler
    Sampler initialized = TraceSamplingRatePolicy.getInitializedSampler();
    return initialized == null ? Sampler.parentBased(Sampler.alwaysOn()) : initialized;
  }

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  private static final class SystemPropertyFallbackConfigProperties implements ConfigProperties {
    private static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";
    private static final String OTEL_SERVICE_NAME = "otel.service.name";
    private final ConfigProperties delegate;

    private SystemPropertyFallbackConfigProperties(ConfigProperties delegate) {
      this.delegate = delegate;
    }

    @Override
    @Nullable
    public String getString(String name) {
      String value = safeGetString(name);
      if (value == null && OTEL_SERVICE_NAME.equals(name)) {
        value = getMap(OTEL_RESOURCE_ATTRIBUTES).get("service.name");
      }
      return value != null ? value : getSystemOrEnv(name);
    }

    @Override
    @Nullable
    public Boolean getBoolean(String name) {
      return safeGet(() -> delegate.getBoolean(name));
    }

    @Override
    @Nullable
    public Integer getInt(String name) {
      return safeGet(() -> delegate.getInt(name));
    }

    @Override
    @Nullable
    public Long getLong(String name) {
      return safeGet(() -> delegate.getLong(name));
    }

    @Override
    @Nullable
    public Double getDouble(String name) {
      return safeGet(() -> delegate.getDouble(name));
    }

    @Override
    @Nullable
    public Duration getDuration(String name) {
      return safeGet(() -> delegate.getDuration(name));
    }

    @Override
    public List<String> getList(String name) {
      List<String> value = safeGet(() -> delegate.getList(name));
      return value == null ? Collections.emptyList() : value;
    }

    @Override
    public Map<String, String> getMap(String name) {
      Map<String, String> value = safeGet(() -> delegate.getMap(name));
      if (value != null && !value.isEmpty()) {
        return value;
      }
      if (OTEL_RESOURCE_ATTRIBUTES.equals(name)) {
        value = safeGet(() -> delegate.getMap("resource_attributes"));
        if (value != null && !value.isEmpty()) {
          return value;
        }
      }
      String fallback = getSystemOrEnv(name);
      return fallback == null ? Collections.emptyMap() : parseMap(fallback);
    }

    @Nullable
    private String safeGetString(String name) {
      return safeGet(() -> delegate.getString(name));
    }

    @Nullable
    private static <T> T safeGet(SupplierWithRuntimeException<T> supplier) {
      try {
        return supplier.get();
      } catch (RuntimeException ignored) {
        return null;
      }
    }

    @Nullable
    private static String getSystemOrEnv(String propertyName) {
      String value = System.getProperty(propertyName);
      if (value != null) {
        return value;
      }
      return System.getenv(
          propertyName.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_'));
    }

    private static Map<String, String> parseMap(String value) {
      if (value == null || value.trim().isEmpty()) {
        return Collections.emptyMap();
      }
      Map<String, String> result = new LinkedHashMap<>();
      for (String entry : value.split(",")) {
        int separator = entry.indexOf('=');
        if (separator <= 0) {
          continue;
        }
        result.put(entry.substring(0, separator).trim(), entry.substring(separator + 1).trim());
      }
      return Collections.unmodifiableMap(result);
    }
  }

  @FunctionalInterface
  private interface SupplierWithRuntimeException<T> {
    @Nullable
    T get();
  }
}
