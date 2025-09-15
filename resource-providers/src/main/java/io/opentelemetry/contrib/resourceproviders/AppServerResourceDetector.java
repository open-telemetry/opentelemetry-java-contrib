/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public final class AppServerResourceDetector implements ComponentProvider<Resource> {

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "app_server";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    return new AppServerServiceNameProvider().create();
  }
}
