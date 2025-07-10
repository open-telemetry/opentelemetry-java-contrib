/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

public class GcpResourceDetector implements ComponentProvider<Resource> {

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "gcp";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    ResourceBuilder builder = Resource.builder();
    builder.putAll(new GCPResourceProvider().getAttributes());
    return builder.build();
  }
}
