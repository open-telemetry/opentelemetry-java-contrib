/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CLOUD_PROVIDER;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

public abstract class CloudResourceProvider implements ConditionalResourceProvider {

  @Override
  public final boolean shouldApply(ConfigProperties config, Resource existing) {
    return existing.getAttribute(CLOUD_PROVIDER) == null;
  }

  @Override
  public final Resource createResource(ConfigProperties config) {
    // not using config in any providers
    return createResource();
  }

  abstract Resource createResource();
}
