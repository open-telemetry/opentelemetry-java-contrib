/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_AVAILABILITY_ZONE;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_CLOUD_REGION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_GROUP_MANAGER_NAME;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_GROUP_MANAGER_REGION;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_GROUP_MANAGER_ZONE;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_HOSTNAME;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_ID;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_INSTANCE_NAME;
import static io.opentelemetry.contrib.gcp.resource.AttributeKeys.GCE_MACHINE_TYPE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GoogleComputeEngine implements DetectedPlatform {
  // Format described in:
  // https://cloud.google.com/compute/docs/instance-groups/getting-info-about-migs#checking_if_a_vm_instance_is_part_of_a_mig
  private static final Pattern CREATED_BY_MIG_PATTERN =
      Pattern.compile("^projects/[^/]+/(zones|regions)/([^/]+)/instanceGroupManagers/([^/]+)$");

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

    String createdBy = this.metadataConfig.getCreatedBy();
    if (createdBy != null) {
      Matcher matcher = CREATED_BY_MIG_PATTERN.matcher(createdBy);
      if (matcher.matches()) {
        map.put(GCE_INSTANCE_GROUP_MANAGER_NAME, matcher.group(3));
        if ("zones".equals(matcher.group(1))) {
          map.put(GCE_INSTANCE_GROUP_MANAGER_ZONE, matcher.group(2));
        } else {
          map.put(GCE_INSTANCE_GROUP_MANAGER_REGION, matcher.group(2));
        }
      }
    }
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
