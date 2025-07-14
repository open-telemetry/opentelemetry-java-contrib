/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sdk.autoconfigure;

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.SdkConfigProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.util.Collections;
import java.util.Map;

public class ConfigPropertiesUtil {
  private ConfigPropertiesUtil() {}

  /** Resolve {@link ConfigProperties} from the {@code autoConfiguredOpenTelemetrySdk}. */
  public static ConfigProperties resolveConfigProperties(
      AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties sdkConfigProperties =
        AutoConfigureUtil.getConfig(autoConfiguredOpenTelemetrySdk);
    if (sdkConfigProperties != null) {
      return sdkConfigProperties;
    }
    ConfigProvider configProvider =
        AutoConfigureUtil.getConfigProvider(autoConfiguredOpenTelemetrySdk);
    if (configProvider != null) {
      DeclarativeConfigProperties instrumentationConfig = configProvider.getInstrumentationConfig();

      if (instrumentationConfig == null) {
        instrumentationConfig = DeclarativeConfigProperties.empty();
      }

      return new DeclarativeConfigPropertiesBridge(instrumentationConfig, Collections.emptyMap());
    }
    // Should never happen
    throw new IllegalStateException(
        "AutoConfiguredOpenTelemetrySdk does not have ConfigProperties or DeclarativeConfigProperties. This is likely a programming error in opentelemetry-java");
  }

  public static ConfigProperties resolveModel(OpenTelemetryConfigurationModel model) {
    return resolveModel(model, Collections.emptyMap());
  }

  public static ConfigProperties resolveModel(
      OpenTelemetryConfigurationModel model, Map<String, String> translationMap) {
    SdkConfigProvider configProvider = SdkConfigProvider.create(model);
    DeclarativeConfigProperties instrumentationConfig = configProvider.getInstrumentationConfig();
    if (instrumentationConfig == null) {
      instrumentationConfig = DeclarativeConfigProperties.empty();
    }

    return new DeclarativeConfigPropertiesBridge(instrumentationConfig, translationMap);
  }

  public static String propertyYamlPath(String propertyName) {
    return DeclarativeConfigPropertiesBridge.yamlPath(propertyName);
  }
}
