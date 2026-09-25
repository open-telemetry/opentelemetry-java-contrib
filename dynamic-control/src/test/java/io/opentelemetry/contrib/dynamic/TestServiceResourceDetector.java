/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

/** Test-only detector with an identity that is absent from YAML and ambient properties. */
public final class TestServiceResourceDetector implements ComponentProvider {
  @Override
  public String getName() {
    return "test_service";
  }

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    return Resource.builder().put("service.name", "detected-service").build();
  }
}
