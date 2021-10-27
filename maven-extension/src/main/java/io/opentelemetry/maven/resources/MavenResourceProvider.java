/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.resources;

import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class MavenResourceProvider implements ResourceProvider {
  @Override
  public Resource createResource(ConfigProperties config) {
    ResourceBuilder resourceBuilder = Resource.builder();
    resourceBuilder.put(
        ResourceAttributes.SERVICE_NAME,
        MavenOtelSemanticAttributes.ServiceNameValues.SERVICE_NAME_VALUE);
    return resourceBuilder.build();
  }
}
