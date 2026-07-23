/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Adds the map-shaped legacy properties that the flat-to-declarative bridge cannot expose. */
final class LegacyConfigPropertiesBridge {
  private static final String OPAMP_HEADERS = "otel.experimental.opamp.headers";
  private static final String RESOURCE_ATTRIBUTES = "otel.resource.attributes";

  private LegacyConfigPropertiesBridge() {}

  static DeclarativeConfigProperties create(
      ConfigProperties configProperties, DeclarativeConfigProperties delegate) {
    return new RootProperties(configProperties, delegate);
  }

  static Map<String, String> getOpampHeaders(ConfigProperties configProperties) {
    Map<String, String> headers = configProperties.getMap(OPAMP_HEADERS);
    return headers.isEmpty()
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(new HashMap<>(headers));
  }

  private static final class RootProperties implements DeclarativeConfigProperties {
    private final ConfigProperties configProperties;
    private final DeclarativeConfigProperties delegate;

    private RootProperties(
        ConfigProperties configProperties, DeclarativeConfigProperties delegate) {
      this.configProperties = configProperties;
      this.delegate = delegate;
    }

    @Override
    @Nullable
    public String getString(String name) {
      return delegate.getString(name);
    }

    @Override
    @Nullable
    public Boolean getBoolean(String name) {
      return delegate.getBoolean(name);
    }

    @Override
    @Nullable
    public Integer getInt(String name) {
      return delegate.getInt(name);
    }

    @Override
    @Nullable
    public Long getLong(String name) {
      return delegate.getLong(name);
    }

    @Override
    @Nullable
    public Double getDouble(String name) {
      return delegate.getDouble(name);
    }

    @Override
    @Nullable
    public <T> List<T> getScalarList(String name, Class<T> scalarType) {
      return delegate.getScalarList(name, scalarType);
    }

    @Override
    @Nullable
    public DeclarativeConfigProperties getStructured(String name) {
      if (RESOURCE_ATTRIBUTES.equals(name)) {
        return new MapProperties(configProperties.getMap(name), delegate.getComponentLoader());
      }
      return delegate.getStructured(name);
    }

    @Override
    @Nullable
    public List<DeclarativeConfigProperties> getStructuredList(String name) {
      return delegate.getStructuredList(name);
    }

    @Override
    public Set<String> getPropertyKeys() {
      return delegate.getPropertyKeys();
    }

    @Override
    public ComponentLoader getComponentLoader() {
      return delegate.getComponentLoader();
    }
  }

  private static final class MapProperties implements DeclarativeConfigProperties {
    private final Map<String, String> values;
    private final ComponentLoader componentLoader;

    private MapProperties(Map<String, String> values, ComponentLoader componentLoader) {
      this.values =
          values == null
              ? Collections.emptyMap()
              : Collections.unmodifiableMap(new HashMap<>(values));
      this.componentLoader = componentLoader;
    }

    @Override
    @Nullable
    public String getString(String name) {
      return values.get(name);
    }

    @Override
    @Nullable
    public Boolean getBoolean(String name) {
      return null;
    }

    @Override
    @Nullable
    public Integer getInt(String name) {
      return null;
    }

    @Override
    @Nullable
    public Long getLong(String name) {
      return null;
    }

    @Override
    @Nullable
    public Double getDouble(String name) {
      return null;
    }

    @Override
    @Nullable
    public <T> List<T> getScalarList(String name, Class<T> scalarType) {
      return null;
    }

    @Override
    @Nullable
    public DeclarativeConfigProperties getStructured(String name) {
      return null;
    }

    @Override
    @Nullable
    public List<DeclarativeConfigProperties> getStructuredList(String name) {
      return null;
    }

    @Override
    public Set<String> getPropertyKeys() {
      return values.keySet();
    }

    @Override
    public ComponentLoader getComponentLoader() {
      return componentLoader;
    }
  }
}
