/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import static com.google.cloud.opentelemetry.detection.AttributeKeys.GAE_APP_VERSION;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GAE_AVAILABILITY_ZONE;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GAE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GAE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GAE_MODULE_NAME;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCE_AVAILABILITY_ZONE;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCE_INSTANCE_HOSTNAME;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCE_INSTANCE_NAME;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCE_MACHINE_TYPE;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GKE_CLUSTER_LOCATION;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GKE_CLUSTER_LOCATION_TYPE;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GKE_CLUSTER_NAME;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GKE_HOST_ID;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GKE_LOCATION_TYPE_REGION;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GKE_LOCATION_TYPE_ZONE;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.SERVERLESS_COMPUTE_AVAILABILITY_ZONE;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.SERVERLESS_COMPUTE_NAME;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.SERVERLESS_COMPUTE_REVISION;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_ACCOUNT_ID;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CloudPlatformValues.GCP_APP_ENGINE;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CloudPlatformValues.GCP_CLOUD_RUN;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CloudProviderValues.GCP;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_INSTANCE;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_NAME;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_VERSION;
import static io.opentelemetry.semconv.incubating.GcpIncubatingAttributes.GCP_GCE_INSTANCE_HOSTNAME;
import static io.opentelemetry.semconv.incubating.GcpIncubatingAttributes.GCP_GCE_INSTANCE_NAME;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_NAME;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_TYPE;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_CLUSTER_NAME;

import com.google.cloud.opentelemetry.detection.DetectedPlatform;
import com.google.cloud.opentelemetry.detection.GCPPlatformDetector;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class GCPResourceProvider implements ResourceProvider {

  private static final Logger LOGGER = Logger.getLogger(GCPResourceProvider.class.getSimpleName());
  private final GCPPlatformDetector detector;

  // for testing only
  GCPResourceProvider(GCPPlatformDetector detector) {
    this.detector = detector;
  }

  public GCPResourceProvider() {
    this.detector = GCPPlatformDetector.DEFAULT_INSTANCE;
  }

  /**
   * Generates and returns the attributes for the resource. The attributes vary depending on the
   * type of resource detected.
   *
   * @return The {@link Attributes} for the detected resource.
   */
  public Attributes getAttributes() {
    DetectedPlatform detectedPlatform = detector.detectPlatform();
    if (detectedPlatform.getSupportedPlatform()
        == GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM) {
      return Attributes.empty();
    }

    // This is running on some sort of GCPCompute - figure out the platform
    AttributesBuilder attrBuilder = Attributes.builder();
    attrBuilder.put(CLOUD_PROVIDER, GCP);
    attrBuilder.put(CLOUD_ACCOUNT_ID, detectedPlatform.getProjectId());

    switch (detectedPlatform.getSupportedPlatform()) {
      case GOOGLE_KUBERNETES_ENGINE:
        addGkeAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_CLOUD_RUN:
        addGcrAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_CLOUD_FUNCTIONS:
        addGcfAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_APP_ENGINE:
        addGaeAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      case GOOGLE_COMPUTE_ENGINE:
        addGceAttributes(attrBuilder, detectedPlatform.getAttributes());
        break;
      default:
        // We don't support this platform yet, so just return with what we have
    }

    return attrBuilder.build();
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    return Resource.create(getAttributes());
  }

  /**
   * Updates the attributes builder with required attributes for GCE resource, if GCE resource is
   * applicable. By default, if the resource is running on GCP, it is assumed to be GCE. This means
   * additional attributes are added/overwritten if later on, the resource is identified to be some
   * other platform - like GKE, GAE, etc.
   */
  private static void addGceAttributes(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(CLOUD_PLATFORM, GCP_COMPUTE_ENGINE);

    Optional.ofNullable(attributesMap.get(GCE_AVAILABILITY_ZONE))
        .ifPresent(zone -> attrBuilder.put(CLOUD_AVAILABILITY_ZONE, zone));
    Optional.ofNullable(attributesMap.get(GCE_CLOUD_REGION))
        .ifPresent(region -> attrBuilder.put(CLOUD_REGION, region));
    Optional.ofNullable(attributesMap.get(GCE_INSTANCE_ID))
        .ifPresent(instanceId -> attrBuilder.put(HOST_ID, instanceId));
    Optional.ofNullable(attributesMap.get(GCE_INSTANCE_NAME))
        .ifPresent(
            instanceName -> {
              attrBuilder.put(HOST_NAME, instanceName);
              attrBuilder.put(GCP_GCE_INSTANCE_NAME, instanceName);
            });
    Optional.ofNullable(attributesMap.get(GCE_INSTANCE_HOSTNAME))
        .ifPresent(
            instanceHostname -> attrBuilder.put(GCP_GCE_INSTANCE_HOSTNAME, instanceHostname));
    Optional.ofNullable(attributesMap.get(GCE_MACHINE_TYPE))
        .ifPresent(machineType -> attrBuilder.put(HOST_TYPE, machineType));
  }

  /**
   * Updates the attributes with the required keys for a GKE (Google Kubernetes Engine) environment.
   * The attributes are not updated in case the environment is not deemed to be GKE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private static void addGkeAttributes(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(CLOUD_PLATFORM, GCP_KUBERNETES_ENGINE);

    Optional.ofNullable(attributesMap.get(GKE_CLUSTER_NAME))
        .ifPresent(clusterName -> attrBuilder.put(K8S_CLUSTER_NAME, clusterName));
    Optional.ofNullable(attributesMap.get(GKE_HOST_ID))
        .ifPresent(hostId -> attrBuilder.put(HOST_ID, hostId));
    Optional.ofNullable(attributesMap.get(GKE_CLUSTER_LOCATION_TYPE))
        .ifPresent(
            locationType -> {
              if (attributesMap.get(GKE_CLUSTER_LOCATION) != null) {
                switch (locationType) {
                  case GKE_LOCATION_TYPE_REGION:
                    attrBuilder.put(CLOUD_REGION, attributesMap.get(GKE_CLUSTER_LOCATION));
                    break;
                  case GKE_LOCATION_TYPE_ZONE:
                    attrBuilder.put(
                        CLOUD_AVAILABILITY_ZONE, attributesMap.get(GKE_CLUSTER_LOCATION));
                    break;
                  default:
                    // TODO: Figure out how to handle unexpected conditions like this
                    LOGGER.severe(
                        String.format(
                            "Unrecognized format for cluster location: %s",
                            attributesMap.get(GKE_CLUSTER_LOCATION)));
                }
              }
            });
  }

  /**
   * Updates the attributes with the required keys for a GCR (Google Cloud Run) environment. The
   * attributes are not updated in case the environment is not deemed to be GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private static void addGcrAttributes(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(CLOUD_PLATFORM, GCP_CLOUD_RUN);
    addCommonAttributesForServerlessCompute(attrBuilder, attributesMap);
  }

  /**
   * Updates the attributes with the required keys for a GCF (Google Cloud Functions) environment.
   * The attributes are not updated in case the environment is not deemed to be GCF.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private static void addGcfAttributes(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(CLOUD_PLATFORM, GCP_CLOUD_FUNCTIONS);
    addCommonAttributesForServerlessCompute(attrBuilder, attributesMap);
  }

  /**
   * Updates the attributes with the required keys for a GAE (Google App Engine) environment. The
   * attributes are not updated in case the environment is not deemed to be GAE.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private static void addGaeAttributes(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    attrBuilder.put(CLOUD_PLATFORM, GCP_APP_ENGINE);
    Optional.ofNullable(attributesMap.get(GAE_MODULE_NAME))
        .ifPresent(appName -> attrBuilder.put(FAAS_NAME, appName));
    Optional.ofNullable(attributesMap.get(GAE_APP_VERSION))
        .ifPresent(appVersion -> attrBuilder.put(FAAS_VERSION, appVersion));
    Optional.ofNullable(attributesMap.get(GAE_INSTANCE_ID))
        .ifPresent(appInstanceId -> attrBuilder.put(FAAS_INSTANCE, appInstanceId));
    Optional.ofNullable(attributesMap.get(GAE_CLOUD_REGION))
        .ifPresent(cloudRegion -> attrBuilder.put(CLOUD_REGION, cloudRegion));
    Optional.ofNullable(attributesMap.get(GAE_AVAILABILITY_ZONE))
        .ifPresent(
            cloudAvailabilityZone ->
                attrBuilder.put(CLOUD_AVAILABILITY_ZONE, cloudAvailabilityZone));
  }

  /**
   * This function adds common attributes required for most serverless compute platforms within GCP.
   * Currently, these attributes are required for both GCF and GCR.
   *
   * @param attrBuilder The {@link AttributesBuilder} object that needs to be updated with the
   *     necessary keys.
   */
  private static void addCommonAttributesForServerlessCompute(
      AttributesBuilder attrBuilder, Map<String, String> attributesMap) {
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_NAME))
        .ifPresent(name -> attrBuilder.put(FAAS_NAME, name));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_REVISION))
        .ifPresent(revision -> attrBuilder.put(FAAS_VERSION, revision));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_INSTANCE_ID))
        .ifPresent(instanceId -> attrBuilder.put(FAAS_INSTANCE, instanceId));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE))
        .ifPresent(zone -> attrBuilder.put(CLOUD_AVAILABILITY_ZONE, zone));
    Optional.ofNullable(attributesMap.get(SERVERLESS_COMPUTE_CLOUD_REGION))
        .ifPresent(region -> attrBuilder.put(CLOUD_REGION, region));
  }
}
