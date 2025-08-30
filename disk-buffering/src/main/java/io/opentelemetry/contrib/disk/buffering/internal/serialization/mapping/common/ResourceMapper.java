/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import javax.annotation.Nullable;

public final class ResourceMapper {

  private static final ResourceMapper INSTANCE = new ResourceMapper();

  public static ResourceMapper getInstance() {
    return INSTANCE;
  }

  public Resource mapToProto(io.opentelemetry.sdk.resources.Resource sdkResource) {
    return new Resource.Builder()
        .attributes(AttributesMapper.getInstance().attributesToProto(sdkResource.getAttributes()))
        .build();
  }

  public io.opentelemetry.sdk.resources.Resource mapToSdk(
      Resource protoResource, @Nullable String schemaUrl) {
    ResourceBuilder resource = io.opentelemetry.sdk.resources.Resource.builder();

    if (schemaUrl != null) {
      resource.setSchemaUrl(schemaUrl);
    }
    resource.putAll(AttributesMapper.getInstance().protoToAttributes(protoResource.attributes));
    return resource.build();
  }
}
