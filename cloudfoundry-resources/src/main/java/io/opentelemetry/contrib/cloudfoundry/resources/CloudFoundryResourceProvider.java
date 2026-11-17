/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.cloudfoundry.resources;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

/** {@link ResourceProvider} for {@link CloudFoundryResource}. */
public final class CloudFoundryResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties configProperties) {
    return CloudFoundryResource.get();
  }
}
