/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

abstract class CloudResourceProvider implements ConditionalResourceProvider {
  @Override
  public final boolean shouldApply(ConfigProperties config, Resource existing) {
    return existing.getAttribute(CLOUD_PROVIDER) == null;
  }
}
