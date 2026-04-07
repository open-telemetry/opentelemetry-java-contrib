/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GKE_CLUSTER_LOCATION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GKE_CLUSTER_LOCATION_TYPE;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GKE_CLUSTER_NAME;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GKE_HOST_ID;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GKE_LOCATION_TYPE_REGION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GKE_LOCATION_TYPE_ZONE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class GoogleKubernetesEngine implements DetectedPlatform {
  private final GcpMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleKubernetesEngine(GcpMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(GKE_CLUSTER_NAME, this.metadataConfig.getClusterName());
    map.put(GKE_CLUSTER_LOCATION, this.metadataConfig.getClusterLocation());
    map.put(GKE_CLUSTER_LOCATION_TYPE, this.getClusterLocationType());
    map.put(GKE_HOST_ID, this.metadataConfig.getInstanceId());
    return Collections.unmodifiableMap(map);
  }

  private String getClusterLocationType() {
    String clusterLocation = this.metadataConfig.getClusterLocation();
    long dashCount =
        (clusterLocation == null || clusterLocation.isEmpty())
            ? 0
            : clusterLocation.chars().filter(ch -> ch == '-').count();
    if (dashCount == 1) {
      return GKE_LOCATION_TYPE_REGION;
    } else if (dashCount == 2) {
      return GKE_LOCATION_TYPE_ZONE;
    }
    return "";
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE;
  }

  @Override
  public String getProjectId() {
    return this.metadataConfig.getProjectId();
  }

  @Override
  public Map<String, String> getAttributes() {
    return this.availableAttributes;
  }
}
