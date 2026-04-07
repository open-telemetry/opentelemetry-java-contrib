/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * GoogleServerlessCompute adds attributes applicable to all serverless compute platforms in GCP.
 * Currently, this includes Google Cloud Functions & Google Cloud Run.
 */
abstract class GoogleServerlessCompute implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GcpMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleServerlessCompute(
      EnvironmentVariables environmentVariables, GcpMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(AttributeKeys.SERVERLESS_COMPUTE_NAME, this.environmentVariables.get("K_SERVICE"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_REVISION, this.environmentVariables.get("K_REVISION"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_AVAILABILITY_ZONE, this.metadataConfig.getZone());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION, this.metadataConfig.getRegionFromZone());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    return Collections.unmodifiableMap(map);
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
