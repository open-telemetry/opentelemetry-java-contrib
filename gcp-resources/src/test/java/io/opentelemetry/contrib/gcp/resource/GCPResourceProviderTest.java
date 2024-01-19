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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.cloud.opentelemetry.detection.DetectedPlatform;
import com.google.cloud.opentelemetry.detection.GCPPlatformDetector;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GCPResourceProviderTest {
  private static final String DUMMY_PROJECT_ID = "google-pid";
  private final ConfigProperties mockConfigProps = Mockito.mock(ConfigProperties.class);
  private final Map<String, String> mockGKECommonAttributes =
      new HashMap<>(
          ImmutableMap.of(
              GKE_CLUSTER_NAME, "gke-cluster",
              GKE_HOST_ID, "host1"));

  // Mock Platforms
  private static DetectedPlatform generateMockGcePlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>(
            ImmutableMap.of(
                GCE_CLOUD_REGION, "australia-southeast1",
                GCE_AVAILABILITY_ZONE, "australia-southeast1-b",
                GCE_INSTANCE_ID, "random-id",
                GCE_INSTANCE_NAME, "instance-name",
                GCE_MACHINE_TYPE, "gce-m2",
                GCE_INSTANCE_HOSTNAME, "instance-hostname"));
    DetectedPlatform mockGCEPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockGCEPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE);
    Mockito.when(mockGCEPlatform.getAttributes()).thenReturn(mockAttributes);
    Mockito.when(mockGCEPlatform.getProjectId()).thenReturn(DUMMY_PROJECT_ID);
    return mockGCEPlatform;
  }

  private DetectedPlatform generateMockGkePlatform(String gkeClusterLocationType) {
    Map<String, String> mockAttributes = new HashMap<>(mockGKECommonAttributes);
    if (gkeClusterLocationType.equals(GKE_LOCATION_TYPE_ZONE)) {
      mockAttributes.put(GKE_CLUSTER_LOCATION, "australia-southeast1-a");
    } else if (gkeClusterLocationType.equals(GKE_LOCATION_TYPE_REGION)) {
      mockAttributes.put(GKE_CLUSTER_LOCATION, "australia-southeast1");
    }
    mockAttributes.put(GKE_CLUSTER_LOCATION_TYPE, gkeClusterLocationType);

    DetectedPlatform mockGKEPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockGKEPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE);
    Mockito.when(mockGKEPlatform.getAttributes()).thenReturn(mockAttributes);
    Mockito.when(mockGKEPlatform.getProjectId()).thenReturn(DUMMY_PROJECT_ID);
    return mockGKEPlatform;
  }

  private static DetectedPlatform generateMockServerlessPlatform(
      GCPPlatformDetector.SupportedPlatform platform) {
    EnumSet<GCPPlatformDetector.SupportedPlatform> serverlessPlatforms =
        EnumSet.of(
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN,
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS);
    if (!serverlessPlatforms.contains(platform)) {
      throw new IllegalArgumentException();
    }
    Map<String, String> mockAttributes =
        new HashMap<>(
            ImmutableMap.of(
                SERVERLESS_COMPUTE_NAME, "serverless-app",
                SERVERLESS_COMPUTE_REVISION, "v2",
                SERVERLESS_COMPUTE_INSTANCE_ID, "serverless-instance-id",
                SERVERLESS_COMPUTE_CLOUD_REGION, "us-central1",
                SERVERLESS_COMPUTE_AVAILABILITY_ZONE, "us-central1-b"));
    DetectedPlatform mockServerlessPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockServerlessPlatform.getSupportedPlatform()).thenReturn(platform);
    Mockito.when(mockServerlessPlatform.getAttributes()).thenReturn(mockAttributes);
    Mockito.when(mockServerlessPlatform.getProjectId()).thenReturn(DUMMY_PROJECT_ID);
    return mockServerlessPlatform;
  }

  private static DetectedPlatform generateMockGaePlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>(
            ImmutableMap.of(
                GAE_MODULE_NAME, "gae-app",
                GAE_APP_VERSION, "v1",
                GAE_INSTANCE_ID, "gae-instance-id",
                GAE_CLOUD_REGION, "us-central1",
                GAE_AVAILABILITY_ZONE, "us-central1-b"));
    DetectedPlatform mockGAEPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockGAEPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_APP_ENGINE);
    Mockito.when(mockGAEPlatform.getAttributes()).thenReturn(mockAttributes);
    Mockito.when(mockGAEPlatform.getProjectId()).thenReturn(DUMMY_PROJECT_ID);
    return mockGAEPlatform;
  }

  private static DetectedPlatform generateMockUnknownPlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>(
            ImmutableMap.of(
                GCE_INSTANCE_ID, "instance-id",
                GCE_CLOUD_REGION, "australia-southeast1"));

    DetectedPlatform mockUnknownPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockUnknownPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM);
    Mockito.when(mockUnknownPlatform.getAttributes()).thenReturn(mockAttributes);
    return mockUnknownPlatform;
  }

  @Test
  public void testGceResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGcePlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_COMPUTE_ENGINE,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_INSTANCE_ID),
        gotResource.getAttributes().get(ResourceAttributes.HOST_ID));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_INSTANCE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.HOST_NAME));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_INSTANCE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.GCP_GCE_INSTANCE_NAME));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_INSTANCE_HOSTNAME),
        gotResource.getAttributes().get(ResourceAttributes.GCP_GCE_INSTANCE_HOSTNAME));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_MACHINE_TYPE),
        gotResource.getAttributes().get(ResourceAttributes.HOST_TYPE));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_AVAILABILITY_ZONE),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GCE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertEquals(10, gotResource.getAttributes().size());
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationTypeRegion() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGkePlatform(GKE_LOCATION_TYPE_REGION);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();

    verifyGkeMapping(gotResource, mockPlatform);
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GKE_CLUSTER_LOCATION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertEquals(6, gotResource.getAttributes().size());
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationTypeZone() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGkePlatform(GKE_LOCATION_TYPE_ZONE);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();

    verifyGkeMapping(gotResource, mockPlatform);
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertEquals(
        mockPlatform.getAttributes().get(GKE_CLUSTER_LOCATION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(6, gotResource.getAttributes().size());
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationTypeInvalid() {
    Map<String, String> mockGKEAttributes = new HashMap<>(mockGKECommonAttributes);
    mockGKEAttributes.put(GKE_CLUSTER_LOCATION_TYPE, "INVALID");
    mockGKEAttributes.put(GKE_CLUSTER_LOCATION, "some-location");

    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE);
    Mockito.when(mockPlatform.getProjectId()).thenReturn(DUMMY_PROJECT_ID);
    Mockito.when(mockPlatform.getAttributes()).thenReturn(mockGKEAttributes);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);

    verifyGkeMapping(gotResource, mockPlatform);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(5, gotResource.getAttributes().size());
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationMissing() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGkePlatform("");
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();
    verifyGkeMapping(gotResource, mockPlatform);
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertNull(gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(5, gotResource.getAttributes().size());
  }

  private static void verifyGkeMapping(Resource gotResource, DetectedPlatform detectedPlatform) {
    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_KUBERNETES_ENGINE,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_HOST_ID),
        gotResource.getAttributes().get(ResourceAttributes.HOST_ID));
    assertEquals(
        detectedPlatform.getAttributes().get(GKE_CLUSTER_NAME),
        gotResource.getAttributes().get(ResourceAttributes.K8S_CLUSTER_NAME));
  }

  @Test
  public void testGcrResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform =
        generateMockServerlessPlatform(GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_CLOUD_RUN,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    verifyServerlessMapping(gotResource, mockPlatform);
    assertEquals(8, gotResource.getAttributes().size());
  }

  @Test
  public void testGcfResourceAttributeMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform =
        generateMockServerlessPlatform(
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_CLOUD_FUNCTIONS,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    verifyServerlessMapping(gotResource, mockPlatform);
    assertEquals(8, gotResource.getAttributes().size());
  }

  private static void verifyServerlessMapping(
      Resource gotResource, DetectedPlatform detectedPlatform) {
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_NAME));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_REVISION),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_VERSION));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_INSTANCE_ID),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_INSTANCE));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        detectedPlatform.getAttributes().get(SERVERLESS_COMPUTE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
  }

  @Test
  public void testGaeResourceAttributeMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGaePlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    Mockito.verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertEquals(
        ResourceAttributes.CloudPlatformValues.GCP_APP_ENGINE,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PLATFORM));
    assertEquals(
        ResourceAttributes.CloudProviderValues.GCP,
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_PROVIDER));
    assertEquals(
        DUMMY_PROJECT_ID, gotResource.getAttributes().get(ResourceAttributes.CLOUD_ACCOUNT_ID));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_MODULE_NAME),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_NAME));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_APP_VERSION),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_VERSION));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_INSTANCE_ID),
        gotResource.getAttributes().get(ResourceAttributes.FAAS_INSTANCE));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_AVAILABILITY_ZONE),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_AVAILABILITY_ZONE));
    assertEquals(
        mockPlatform.getAttributes().get(GAE_CLOUD_REGION),
        gotResource.getAttributes().get(ResourceAttributes.CLOUD_REGION));
    assertEquals(8, gotResource.getAttributes().size());
  }

  @Test
  public void testUnknownPlatformResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockUnknownPlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    assertTrue(gotResource.getAttributes().isEmpty(), "no attributes for unknown platform");
  }

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ResourceProvider> services =
        ServiceLoader.load(ResourceProvider.class, getClass().getClassLoader());
    while (services.iterator().hasNext()) {
      ResourceProvider provider = services.iterator().next();
      if (provider instanceof GCPResourceProvider) {
        return;
      }
    }
    fail("Could not load GCP Resource detector using serviceloader");
  }
}
