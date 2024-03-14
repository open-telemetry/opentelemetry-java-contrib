/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

public abstract class CloudResourceProvider implements ConditionalResourceProvider {

  @Override
  public final boolean shouldApply(ConfigProperties config, Resource existing) {
    return existing.getAttribute(ResourceAttributes.CLOUD_PROVIDER) == null;
  }
}
