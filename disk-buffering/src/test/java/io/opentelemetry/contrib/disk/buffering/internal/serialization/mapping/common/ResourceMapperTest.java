/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.resource.v1.Resource;
import org.junit.jupiter.api.Test;

class ResourceMapperTest {

  @Test
  void verifyMapping() {
    Resource proto = mapToProto(TestData.RESOURCE_FULL);

    assertThat(mapToSdk(proto, TestData.RESOURCE_FULL.getSchemaUrl()))
        .isEqualTo(TestData.RESOURCE_FULL);
  }

  private static Resource mapToProto(io.opentelemetry.sdk.resources.Resource sdkResource) {
    return ResourceMapper.getInstance().mapToProto(sdkResource);
  }

  private static io.opentelemetry.sdk.resources.Resource mapToSdk(
      Resource protoResource, String schemaUrl) {
    return ResourceMapper.getInstance().mapToSdk(protoResource, schemaUrl);
  }
}
