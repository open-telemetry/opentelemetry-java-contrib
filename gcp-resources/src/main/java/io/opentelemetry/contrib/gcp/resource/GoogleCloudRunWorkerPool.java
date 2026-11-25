/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class GoogleCloudRunWorkerPool implements DetectedPlatform {
  private final GcpMetadataConfig metadataConfig;
  private final EnvironmentVariables environmentVariables;
  private final Map<String, String> availableAttributes;

  GoogleCloudRunWorkerPool(
      EnvironmentVariables environmentVariables, GcpMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
    this.environmentVariables = environmentVariables;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_NAME,
        this.environmentVariables.get("CLOUD_RUN_WORKER_POOL"));
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_REVISION,
        this.environmentVariables.get("CLOUD_RUN_REVISION"));
    map.put(AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    map.put(AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION, this.metadataConfig.getRegion());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN_WORKER_POOL;
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
