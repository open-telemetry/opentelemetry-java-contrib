/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Retrieves Google Cloud project-id and a limited set of instance attributes from Metadata server.
 *
 * @see <a href= "https://cloud.google.com/compute/docs/storing-retrieving-metadata">
 *     https://cloud.google.com/compute/docs/storing-retrieving-metadata</a>
 */
final class GcpMetadataConfig {
  static final GcpMetadataConfig DEFAULT_INSTANCE = new GcpMetadataConfig();

  private static final String DEFAULT_URL = "http://metadata.google.internal/computeMetadata/v1/";
  private static final int MAX_METADATA_VALUE_CHARS = 4096;
  // Format described in:
  // https://cloud.google.com/compute/docs/instance-groups/getting-info-about-migs#checking_if_a_vm_instance_is_part_of_a_mig
  private static final Pattern CREATED_BY_MIG_PATTERN =
      Pattern.compile("^projects/[^/]+/(zones|regions)/([^/]+)/instanceGroupManagers/([^/]+)$");
  private final String url;
  private final Map<String, String> cachedAttributes = new ConcurrentHashMap<>();

  private GcpMetadataConfig() {
    this.url = DEFAULT_URL;
  }

  // For testing only
  GcpMetadataConfig(String url) {
    this.url = url;
  }

  // Returns null on failure to retrieve from metadata server
  String getProjectId() {
    return getAttribute("project/project-id");
  }

  /**
   * Method to extract cloud availability zone from the metadata server.
   *
   * <p>Example response: projects/640212054955/zones/australia-southeast1-a
   *
   * <p>Example zone: australia-southeast1-a
   *
   * @return the extracted zone from the metadata server response or null in case of failure to
   *     retrieve from metadata server.
   */
  String getZone() {
    String zone = getAttribute("instance/zone");
    if (zone != null && zone.contains("/")) {
      zone = zone.substring(zone.lastIndexOf('/') + 1);
    }
    return zone;
  }

  /**
   * Use this method only when the region cannot be parsed from the zone. Known use-cases of this
   * method involve detecting region in GAE standard environment.
   *
   * <p>Example response: projects/5689182099321/regions/us-central1.
   *
   * @return the retrieved region or null in case of failure to retrieve from metadata server
   */
  String getRegion() {
    String region = getAttribute("instance/region");
    if (region != null && region.contains("/")) {
      region = region.substring(region.lastIndexOf('/') + 1);
    }
    return region;
  }

  /**
   * Use this method to parse region from zone.
   *
   * <p>Example region: australia-southeast1
   *
   * @return parsed region from the zone, if zone is not found or is invalid, this method returns
   *     null.
   */
  @Nullable
  String getRegionFromZone() {
    String region = null;
    String zone = getZone();
    if (zone != null && !zone.isEmpty()) {
      // Parsing required to scope up to a region
      String[] splitArr = zone.split("-");
      if (splitArr.length > 2) {
        region = String.join("-", splitArr[0], splitArr[1]);
      }
    }
    return region;
  }

  // Example response: projects/640212054955/machineTypes/e2-medium
  String getMachineType() {
    String machineType = getAttribute("instance/machine-type");
    if (machineType != null && machineType.contains("/")) {
      machineType = machineType.substring(machineType.lastIndexOf('/') + 1);
    }
    return machineType;
  }

  // Returns null on failure to retrieve from metadata server
  String getInstanceId() {
    return getAttribute("instance/id");
  }

  // Returns null on failure to retrieve from metadata server
  String getClusterName() {
    return getAttribute("instance/attributes/cluster-name");
  }

  // Returns null on failure to retrieve from metadata server
  String getClusterLocation() {
    return getAttribute("instance/attributes/cluster-location");
  }

  // Returns null on failure to retrieve from metadata server
  String getInstanceHostName() {
    return getAttribute("instance/hostname");
  }

  // Returns null on failure to retrieve from metadata server
  String getInstanceName() {
    return getAttribute("instance/name");
  }

  // Returns null on failure to retrieve from metadata server
  String getCreatedBy() {
    return getAttribute("instance/attributes/created-by");
  }

  /**
   * Parses the managed instance group (MIG) name from the {@code instance/attributes/created-by}
   * metadata attribute.
   *
   * <p>Example response: {@code
   * projects/123456789/zones/us-central1-a/instanceGroupManagers/my-mig}
   *
   * @return parsed MIG name, or null if the instance was not created by a MIG.
   */
  @Nullable
  String getMigNameFromCreatedBy() {
    Matcher matcher = matchCreatedByMig();
    return matcher != null ? matcher.group(3) : null;
  }

  /**
   * Parses the managed instance group (MIG) zone from the {@code instance/attributes/created-by}
   * metadata attribute for zonal MIGs.
   *
   * @return parsed MIG zone, or null if the instance was not created by a zonal MIG.
   */
  @Nullable
  String getMigZoneFromCreatedBy() {
    Matcher matcher = matchCreatedByMig();
    return matcher != null && "zones".equals(matcher.group(1)) ? matcher.group(2) : null;
  }

  /**
   * Parses the managed instance group (MIG) region from the {@code instance/attributes/created-by}
   * metadata attribute for regional MIGs.
   *
   * @return parsed MIG region, or null if the instance was not created by a regional MIG.
   */
  @Nullable
  String getMigRegionFromCreatedBy() {
    Matcher matcher = matchCreatedByMig();
    return matcher != null && "regions".equals(matcher.group(1)) ? matcher.group(2) : null;
  }

  @Nullable
  private Matcher matchCreatedByMig() {
    String createdBy = getCreatedBy();
    if (createdBy == null) {
      return null;
    }
    Matcher matcher = CREATED_BY_MIG_PATTERN.matcher(createdBy);
    return matcher.matches() ? matcher : null;
  }

  // Returns null on failure to retrieve from metadata server
  private String getAttribute(String attributeName) {
    return cachedAttributes.computeIfAbsent(attributeName, this::fetchAttribute);
  }

  // Return the attribute received at <attributeName> relative path or null on
  // failure
  @Nullable
  private String fetchAttribute(String attributeName) {
    try {
      URL url = URI.create(this.url + attributeName).toURL();
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Metadata-Flavor", "Google");
      if (connection.getResponseCode() == 200
          && "Google".equals(connection.getHeaderField("Metadata-Flavor"))) {
        InputStream input = connection.getInputStream();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
          char[] buffer = new char[MAX_METADATA_VALUE_CHARS];
          int total = 0;
          for (int n;
              total < buffer.length
                  && (n = reader.read(buffer, total, buffer.length - total)) != -1; ) {
            total += n;
          }
          String value = new String(buffer, 0, total).trim();
          return value.isEmpty() ? null : value;
        }
      }
    } catch (IOException ignore) {
      // ignore
    }
    return null;
  }
}
