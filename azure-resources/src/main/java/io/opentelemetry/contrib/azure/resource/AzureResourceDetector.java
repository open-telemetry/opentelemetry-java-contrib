/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

public final class AzureResourceDetector implements ComponentProvider {

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "azure";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    Builder builder = new Builder();
    builder.add(new AzureFunctionsResourceProvider());
    builder.add(new AzureAppServiceResourceProvider());
    builder.add(new AzureContainersResourceProvider());
    builder.addIfEmpty(new AzureAksResourceProvider());
    builder.addIfEmpty(new AzureVmResourceProvider());
    return builder.builder.build();
  }

  private static class Builder {
    final ResourceBuilder builder = Resource.builder();
    int attributesCount = 0;

    private void add(CloudResourceProvider provider) {
      Attributes attributes = provider.createResource().getAttributes();
      builder.putAll(attributes);
      attributesCount += attributes.size();
    }

    private void addIfEmpty(CloudResourceProvider provider) {
      if (attributesCount == 0) {
        add(provider);
      }
    }
  }
}
