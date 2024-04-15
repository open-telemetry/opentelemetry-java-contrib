/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.contrib.aws.resource.EksResource.AUTH_CONFIGMAP_PATH;
import static io.opentelemetry.contrib.aws.resource.EksResource.CW_CONFIGMAP_PATH;
import static io.opentelemetry.contrib.aws.resource.EksResource.K8S_SVC_URL;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.semconv.incubating.CloudIncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.semconv.incubating.ContainerIncubatingAttributes.CONTAINER_ID;
import static io.opentelemetry.semconv.incubating.K8sIncubatingAttributes.K8S_CLUSTER_NAME;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ServiceLoader;
import io.opentelemetry.semconv.SchemaUrls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EksResourceTest {

  @Mock private DockerHelper mockDockerHelper;

  @Mock private SimpleHttpClient httpClient;

  @Test
  void testEks(@TempDir File tempFolder) throws IOException {
    File mockK8sTokenFile = new File(tempFolder, "k8sToken");
    String token = "token123";
    Files.write(token.getBytes(Charsets.UTF_8), mockK8sTokenFile);
    File mockK8sKeystoreFile = new File(tempFolder, "k8sCert");
    String truststore = "truststore123";
    Files.write(truststore.getBytes(Charsets.UTF_8), mockK8sKeystoreFile);

    when(httpClient.fetchString(any(), Mockito.eq(K8S_SVC_URL + AUTH_CONFIGMAP_PATH), any(), any()))
        .thenReturn("not empty");
    when(httpClient.fetchString(any(), Mockito.eq(K8S_SVC_URL + CW_CONFIGMAP_PATH), any(), any()))
        .thenReturn("{\"data\":{\"cluster.name\":\"my-cluster\"}}");
    when(mockDockerHelper.getContainerId()).thenReturn("0123456789A");

    Resource eksResource =
        EksResource.buildResource(
            httpClient,
            mockDockerHelper,
            mockK8sTokenFile.getPath(),
            mockK8sKeystoreFile.getPath());
    Attributes attributes = eksResource.getAttributes();

    assertThat(eksResource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_25_0);
    assertThat(attributes)
        .containsOnly(
            entry(CLOUD_PROVIDER, "aws"),
            entry(CLOUD_PLATFORM, "aws_eks"),
            entry(K8S_CLUSTER_NAME, "my-cluster"),
            entry(CONTAINER_ID, "0123456789A"));
  }

  @Test
  void testNotEks() {
    Resource eksResource = EksResource.buildResource(httpClient, mockDockerHelper, "", "");
    Attributes attributes = eksResource.getAttributes();
    assertThat(attributes).isEmpty();
  }

  @Test
  void inServiceLoader() {
    // No practical way to test the attributes themselves so at least check the service loader picks
    // it up.
    assertThat(ServiceLoader.load(ResourceProvider.class))
        .anyMatch(EksResourceProvider.class::isInstance);
  }
}
