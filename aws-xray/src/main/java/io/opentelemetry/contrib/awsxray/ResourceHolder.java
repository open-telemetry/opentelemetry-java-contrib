/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

/**
 * Currently the only way to read the Resource from autoconfiguration. Best would be if the SPI
 * could return a {@code Function<SamplerFactoryArgs, Sampler>} where SamplerFactoryArgs has
 * SDK-constructed components like Resource and Clock.
 */
@AutoService(AutoConfigurationCustomizerProvider.class)
public final class ResourceHolder implements AutoConfigurationCustomizerProvider {

  @SuppressWarnings("NonFinalStaticField")
  @Nullable static volatile Resource resource;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addResourceCustomizer(
        (resource, config) -> {
          ResourceHolder.resource = resource;
          return resource;
        });
  }

  /**
   * Returns held resource, unless resource is null, in which case {@link Resource#getDefault()} is
   * returned. This should not happen in practice, as {@link #customize} should be automatically
   * run, populating {@link #resource}.
   */
  public static Resource getResource() {
    Resource resourceReference = resource;
    if (resourceReference == null) {
      // Should never be the case in practice.
      resourceReference = Resource.getDefault();
    }
    return resourceReference;
  }
}
