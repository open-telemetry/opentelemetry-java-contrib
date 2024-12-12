/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import io.opentelemetry.semconv.incubating.K8sIncubatingAttributes;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class AzureAksResourceProviderTest extends MetadataBasedResourceProviderTest {

  @NotNull
  @Override
  protected ResourceProvider getResourceProvider(Supplier<Optional<String>> client) {
    return new AzureAksResourceProvider(
        client,
        Collections.singletonMap(AzureAksResourceProvider.KUBERNETES_SERVICE_HOST, "localhost"));
  }

  @Override
  protected String getPlatform() {
    return CloudIncubatingAttributes.CloudPlatformIncubatingValues.AZURE_AKS;
  }

  @Override
  protected void assertDefaultAttributes(AttributesAssert attributesAssert) {
    attributesAssert
        .containsEntry(CLOUD_PROVIDER, "azure")
        .containsEntry(
            CLOUD_PLATFORM, CloudIncubatingAttributes.CloudPlatformIncubatingValues.AZURE_AKS)
        .containsEntry(K8sIncubatingAttributes.K8S_CLUSTER_NAME, "macikgo-test-may-23");
  }

  @Test
  void notOnK8s() {
    AzureAksResourceProvider provider =
        new AzureAksResourceProvider(() -> Optional.of(okResponse()), Collections.emptyMap());
    Attributes attributes = provider.createResource(null).getAttributes();
    OpenTelemetryAssertions.assertThat(attributes).isEmpty();
  }

  @Test
  void parseClusterName() {
    String clusterName =
        AzureAksResourceProvider.parseClusterName(
            "mc_macikgo-test-may-23_macikgo-test-may-23_eastus");
    assertThat(clusterName).isEqualTo("macikgo-test-may-23");
  }
}
