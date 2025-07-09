/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AZURE_AKS;
import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.K8S_CLUSTER_NAME;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class AzureAksResourceProvider extends CloudResourceProvider {

  private static final Map<String, AzureVmResourceProvider.Entry> COMPUTE_MAPPING = new HashMap<>();

  static {
    COMPUTE_MAPPING.put(
        "resourceGroupName",
        new AzureVmResourceProvider.Entry(
            K8S_CLUSTER_NAME, AzureAksResourceProvider::parseClusterName));
  }

  // visible for testing
  static String parseClusterName(String resourceGroup) {
    // Code inspired by
    // https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/datadogexporter/internal/hostmetadata/internal/azure/provider.go#L36
    String[] splitAll = resourceGroup.split("_");
    if (splitAll.length == 4 && splitAll[0].equalsIgnoreCase("mc")) {
      return splitAll[splitAll.length - 2];
    }
    return resourceGroup;
  }

  // Environment variable that is set when running on Kubernetes
  static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
  private final Supplier<Optional<String>> client;
  private final Map<String, String> environment;

  // SPI
  public AzureAksResourceProvider() {
    this(AzureMetadataService.defaultClient(), System.getenv());
  }

  // visible for testing
  AzureAksResourceProvider(Supplier<Optional<String>> client, Map<String, String> environment) {
    this.client = client;
    this.environment = environment;
  }

  @Override
  public int order() {
    // run after the fast cloud resource providers that only check environment variables
    // and before the VM provider
    return 100;
  }

  @Override
  public Resource createResource(ConfigProperties configProperties) {
    if (environment.get(KUBERNETES_SERVICE_HOST) == null) {
      return Resource.empty();
    }
    return client
        .get()
        .map(body -> AzureVmResourceProvider.parseMetadata(body, COMPUTE_MAPPING, AZURE_AKS))
        .orElse(Resource.empty());
  }
}
