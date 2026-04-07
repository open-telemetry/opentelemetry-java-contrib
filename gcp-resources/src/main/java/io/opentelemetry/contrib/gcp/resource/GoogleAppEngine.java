/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GAE_APP_VERSION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GAE_AVAILABILITY_ZONE;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GAE_CLOUD_REGION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GAE_INSTANCE_ID;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GAE_MODULE_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

final class GoogleAppEngine implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GcpMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleAppEngine(EnvironmentVariables environmentVariables, GcpMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(GAE_MODULE_NAME, this.environmentVariables.get("GAE_SERVICE"));
    map.put(GAE_APP_VERSION, this.environmentVariables.get("GAE_VERSION"));
    map.put(GAE_INSTANCE_ID, this.environmentVariables.get("GAE_INSTANCE"));
    map.put(GAE_AVAILABILITY_ZONE, this.metadataConfig.getZone());
    map.put(GAE_CLOUD_REGION, getCloudRegion());
    return Collections.unmodifiableMap(map);
  }

  @Nullable
  private String getCloudRegion() {
    if (this.environmentVariables.get("GAE_ENV") != null
        && this.environmentVariables.get("GAE_ENV").equals("standard")) {
      return this.metadataConfig.getRegion();
    } else {
      return this.metadataConfig.getRegionFromZone();
    }
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_APP_ENGINE;
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
