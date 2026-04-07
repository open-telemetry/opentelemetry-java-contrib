/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_AVAILABILITY_ZONE;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_CLOUD_REGION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_HOSTNAME;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_ID;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_NAME;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_MACHINE_TYPE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class GoogleComputeEngine implements DetectedPlatform {
  private final GcpMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleComputeEngine(GcpMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(GCE_AVAILABILITY_ZONE, this.metadataConfig.getZone());
    map.put(GCE_CLOUD_REGION, this.metadataConfig.getRegionFromZone());
    map.put(GCE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    map.put(GCE_INSTANCE_NAME, this.metadataConfig.getInstanceName());
    map.put(GCE_INSTANCE_HOSTNAME, this.metadataConfig.getInstanceHostName());
    map.put(GCE_MACHINE_TYPE, this.metadataConfig.getMachineType());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE;
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
