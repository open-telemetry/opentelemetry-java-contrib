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
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCR_JOB_EXECUTION_KEY;
import static com.google.cloud.opentelemetry.detection.AttributeKeys.GCR_JOB_TASK_INDEX;
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
import static io.opentelemetry.contrib.gcp.resource.IncubatingAttributes.GCP_CLOUD_RUN_JOB_TASK_INDEX;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
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
import static io.opentelemetry.semconv.incubating.GcpIncubatingAttributes.GCP_CLOUD_RUN_JOB_EXECUTION;
import static io.opentelemetry.semconv.incubating.GcpIncubatingAttributes.GCP_GCE_INSTANCE_HOSTNAME;
import static io.opentelemetry.semconv.incubating.GcpIncubatingAttributes.GCP_GCE_INSTANCE_NAME;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_NAME;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_TYPE;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_CLUSTER_NAME;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

import com.google.cloud.opentelemetry.detection.DetectedPlatform;
import com.google.cloud.opentelemetry.detection.GCPPlatformDetector;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
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

  private static DetectedPlatform generateMockGcrJobPlatform() {
    Map<String, String> mockAttributes =
        new HashMap<>(
            ImmutableMap.of(
                SERVERLESS_COMPUTE_NAME, "serverless-job",
                SERVERLESS_COMPUTE_INSTANCE_ID, "serverless-instance-id",
                SERVERLESS_COMPUTE_CLOUD_REGION, "us-central1",
                GCR_JOB_TASK_INDEX, "1",
                GCR_JOB_EXECUTION_KEY, "serverless-job-a1b2c3"));
    DetectedPlatform mockServerlessPlatform = Mockito.mock(DetectedPlatform.class);
    Mockito.when(mockServerlessPlatform.getSupportedPlatform())
        .thenReturn(GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN_JOB);
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
    Map<String, String> detectedAttributes = mockPlatform.getAttributes();

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertThat(gotResource.getAttributes())
        .hasSize(10)
        .containsEntry(CLOUD_PROVIDER, GCP)
        .containsEntry(CLOUD_PLATFORM, GCP_COMPUTE_ENGINE)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .containsEntry(HOST_ID, detectedAttributes.get(GCE_INSTANCE_ID))
        .containsEntry(HOST_NAME, detectedAttributes.get(GCE_INSTANCE_NAME))
        .containsEntry(GCP_GCE_INSTANCE_NAME, detectedAttributes.get(GCE_INSTANCE_NAME))
        .containsEntry(GCP_GCE_INSTANCE_HOSTNAME, detectedAttributes.get(GCE_INSTANCE_HOSTNAME))
        .containsEntry(HOST_TYPE, detectedAttributes.get(GCE_MACHINE_TYPE))
        .containsEntry(CLOUD_AVAILABILITY_ZONE, detectedAttributes.get(GCE_AVAILABILITY_ZONE))
        .containsEntry(CLOUD_REGION, detectedAttributes.get(GCE_CLOUD_REGION));
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationTypeRegion() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGkePlatform(GKE_LOCATION_TYPE_REGION);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    verifyGkeMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes())
        .hasSize(6)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .containsEntry(CLOUD_REGION, mockPlatform.getAttributes().get(GKE_CLUSTER_LOCATION))
        .doesNotContainKey(CLOUD_AVAILABILITY_ZONE);
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationTypeZone() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGkePlatform(GKE_LOCATION_TYPE_ZONE);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    verifyGkeMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes())
        .hasSize(6)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .containsEntry(
            CLOUD_AVAILABILITY_ZONE, mockPlatform.getAttributes().get(GKE_CLUSTER_LOCATION))
        .doesNotContainKey(CLOUD_REGION);
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
    verify(mockPlatform, Mockito.times(1)).getProjectId();
    assertThat(gotResource.getAttributes())
        .hasSize(5)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .doesNotContainKey(CLOUD_REGION)
        .doesNotContainKey(CLOUD_AVAILABILITY_ZONE);
  }

  @Test
  public void testGkeResourceAttributesMapping_LocationMissing() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGkePlatform("");
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();
    verifyGkeMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes())
        .hasSize(5)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .doesNotContainKey(CLOUD_REGION)
        .doesNotContainKey(CLOUD_AVAILABILITY_ZONE);
  }

  private static void verifyGkeMapping(Resource gotResource, DetectedPlatform detectedPlatform) {
    Map<String, String> detectedAttributes = detectedPlatform.getAttributes();
    assertThat(gotResource.getAttributes())
        .containsEntry(CLOUD_PLATFORM, GCP_KUBERNETES_ENGINE)
        .containsEntry(CLOUD_PROVIDER, GCP)
        .containsEntry(HOST_ID, detectedAttributes.get(GKE_HOST_ID))
        .containsEntry(K8S_CLUSTER_NAME, detectedAttributes.get(GKE_CLUSTER_NAME));
  }

  @Test
  public void testGcrServiceResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform =
        generateMockServerlessPlatform(GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    verifyServerlessMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes())
        .hasSize(8)
        .containsEntry(CLOUD_PLATFORM, GCP_CLOUD_RUN)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID);
  }

  @Test
  public void testGcfResourceAttributeMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform =
        generateMockServerlessPlatform(
            GCPPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS);
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    verifyServerlessMapping(gotResource, mockPlatform);
    assertThat(gotResource.getAttributes())
        .hasSize(8)
        .containsEntry(CLOUD_PLATFORM, GCP_CLOUD_FUNCTIONS)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID);
  }

  private static void verifyServerlessMapping(
      Resource gotResource, DetectedPlatform detectedPlatform) {
    Map<String, String> detectedAttributes = detectedPlatform.getAttributes();
    assertThat(gotResource.getAttributes())
        .containsEntry(CLOUD_PROVIDER, GCP)
        .containsEntry(FAAS_NAME, detectedAttributes.get(SERVERLESS_COMPUTE_NAME))
        .containsEntry(FAAS_VERSION, detectedAttributes.get(SERVERLESS_COMPUTE_REVISION))
        .containsEntry(FAAS_INSTANCE, detectedAttributes.get(SERVERLESS_COMPUTE_INSTANCE_ID))
        .containsEntry(
            CLOUD_AVAILABILITY_ZONE, detectedAttributes.get(SERVERLESS_COMPUTE_AVAILABILITY_ZONE))
        .containsEntry(CLOUD_REGION, detectedAttributes.get(SERVERLESS_COMPUTE_CLOUD_REGION));
  }

  @Test
  public void testGcrJobResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGcrJobPlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);
    Map<String, String> detectedAttributes = mockPlatform.getAttributes();

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertThat(gotResource.getAttributes())
        .hasSize(8)
        .containsEntry(CLOUD_PROVIDER, GCP)
        .containsEntry(CLOUD_PLATFORM, GCP_CLOUD_RUN)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .containsEntry(FAAS_NAME, detectedAttributes.get(SERVERLESS_COMPUTE_NAME))
        .containsEntry(FAAS_NAME, detectedAttributes.get(SERVERLESS_COMPUTE_NAME))
        .containsEntry(FAAS_INSTANCE, detectedAttributes.get(SERVERLESS_COMPUTE_INSTANCE_ID))
        .containsEntry(GCP_CLOUD_RUN_JOB_EXECUTION, detectedAttributes.get(GCR_JOB_EXECUTION_KEY))
        .containsEntry(
            GCP_CLOUD_RUN_JOB_TASK_INDEX,
            Integer.parseInt(detectedAttributes.get(GCR_JOB_TASK_INDEX)))
        .containsEntry(CLOUD_REGION, detectedAttributes.get(SERVERLESS_COMPUTE_CLOUD_REGION));
  }

  @Test
  public void testGaeResourceAttributeMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockGaePlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);
    Map<String, String> detectedAttributes = mockPlatform.getAttributes();

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    verify(mockPlatform, Mockito.times(1)).getProjectId();

    assertThat(gotResource.getAttributes())
        .hasSize(8)
        .containsEntry(CLOUD_PROVIDER, GCP)
        .containsEntry(CLOUD_PLATFORM, GCP_APP_ENGINE)
        .containsEntry(CLOUD_ACCOUNT_ID, DUMMY_PROJECT_ID)
        .containsEntry(FAAS_NAME, detectedAttributes.get(GAE_MODULE_NAME))
        .containsEntry(FAAS_VERSION, detectedAttributes.get(GAE_APP_VERSION))
        .containsEntry(FAAS_INSTANCE, detectedAttributes.get(GAE_INSTANCE_ID))
        .containsEntry(CLOUD_AVAILABILITY_ZONE, detectedAttributes.get(GAE_AVAILABILITY_ZONE))
        .containsEntry(CLOUD_REGION, detectedAttributes.get(GAE_CLOUD_REGION));
  }

  @Test
  public void testUnknownPlatformResourceAttributesMapping() {
    GCPPlatformDetector mockDetector = Mockito.mock(GCPPlatformDetector.class);
    DetectedPlatform mockPlatform = generateMockUnknownPlatform();
    Mockito.when(mockDetector.detectPlatform()).thenReturn(mockPlatform);

    Resource gotResource = new GCPResourceProvider(mockDetector).createResource(mockConfigProps);
    assertThat(gotResource.getAttributes()).isEmpty();
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
