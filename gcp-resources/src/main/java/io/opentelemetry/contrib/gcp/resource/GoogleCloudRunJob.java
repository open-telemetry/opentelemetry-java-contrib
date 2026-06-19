/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class GoogleCloudRunJob implements DetectedPlatform {
  private final GcpMetadataConfig metadataConfig;
  private final EnvironmentVariables environmentVariables;
  private final Map<String, String> availableAttributes;

  GoogleCloudRunJob(EnvironmentVariables environmentVariables, GcpMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
    this.environmentVariables = environmentVariables;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(AttributeKeys.SERVERLESS_COMPUTE_NAME, this.environmentVariables.get("CLOUD_RUN_JOB"));
    map.put(
        AttributeKeys.GCR_JOB_EXECUTION_KEY, this.environmentVariables.get("CLOUD_RUN_EXECUTION"));
    map.put(
        AttributeKeys.GCR_JOB_TASK_INDEX, this.environmentVariables.get("CLOUD_RUN_TASK_INDEX"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION, this.metadataConfig.getRegionFromZone());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN_JOB;
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
