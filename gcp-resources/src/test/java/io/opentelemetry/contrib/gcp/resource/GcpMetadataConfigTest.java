/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@WireMockTest(httpPort = 8090)
class GcpMetadataConfigTest {
  private static final String mockProjectId = "pid";
  private static final String mockZone = "country-region-zone";
  private static final String mockRegion = "country-region1";
  private static final String mockInstanceId = "instance-id";
  private static final String mockInstanceName = "instance-name";
  private static final String mockInstanceType = "instance-type";
  private static final String mockClusterName = "cluster-name";
  private static final String mockClusterLocation = "cluster-location";
  private static final String mockHostname = "hostname";

  private final GcpMetadataConfig mockMetadataConfig =
      new GcpMetadataConfig("http://localhost:8090/");

  @BeforeEach
  public void setupMockMetadataConfig() {
    TestUtils.stubEndpoint("/project/project-id", mockProjectId);
    TestUtils.stubEndpoint("/instance/zone", mockZone);
    TestUtils.stubEndpoint("/instance/region", mockRegion);
    TestUtils.stubEndpoint("/instance/id", mockInstanceId);
    TestUtils.stubEndpoint("/instance/name", mockInstanceName);
    TestUtils.stubEndpoint("/instance/machine-type", mockInstanceType);
    TestUtils.stubEndpoint("/instance/attributes/cluster-name", mockClusterName);
    TestUtils.stubEndpoint("/instance/attributes/cluster-location", mockClusterLocation);
    TestUtils.stubEndpoint("/instance/hostname", mockHostname);
  }

  @Test
  void testGetProjectId() {
    assertEquals(mockProjectId, mockMetadataConfig.getProjectId());
  }

  /** Test Zone Retrieval */
  @ParameterizedTest
  @MethodSource("provideZoneRetrievalArguments")
  void testGetZone(String stubbedMockZone, String expectedMockZone) {
    TestUtils.stubEndpoint("/instance/zone", stubbedMockZone);
    assertEquals(expectedMockZone, mockMetadataConfig.getZone());
  }

  private static Stream<Arguments> provideZoneRetrievalArguments() {
    return Stream.of(
        Arguments.of(mockZone, mockZone),
        Arguments.of(
            "projects/640212054955/zones/australia-southeast1-a", "australia-southeast1-a"),
        Arguments.of("", null),
        Arguments.of(null, null));
  }

  /** Test Region Retrieval */
  @ParameterizedTest
  @MethodSource("provideRegionRetrievalArguments")
  void testGetRegion(String stubbedMockRegion, String expectedMockRegion) {
    TestUtils.stubEndpoint("/instance/region", stubbedMockRegion);
    assertEquals(expectedMockRegion, mockMetadataConfig.getRegion());
  }

  private static Stream<Arguments> provideRegionRetrievalArguments() {
    return Stream.of(
        Arguments.of(mockRegion, mockRegion),
        Arguments.of("projects/640212054955/regions/us-central1", "us-central1"),
        Arguments.of("", null),
        Arguments.of(null, null));
  }

  /** Test Region Retrieval from Zone */
  @ParameterizedTest
  @MethodSource("provideZoneArguments")
  void testGetRegionFromZone(String stubbedMockZone, String expectedRegion) {
    TestUtils.stubEndpoint("/instance/zone", stubbedMockZone);
    assertEquals(expectedRegion, mockMetadataConfig.getRegionFromZone());
  }

  private static Stream<Arguments> provideZoneArguments() {
    return Stream.of(
        Arguments.of(mockZone, "country-region"),
        Arguments.of("projects/640212054955/zones/australia-southeast1-a", "australia-southeast1"),
        Arguments.of("country-region", null),
        Arguments.of("", null),
        Arguments.of(null, null));
  }

  /** Test Machine Type Retrieval */
  @ParameterizedTest
  @MethodSource("provideMachineTypeRetrievalArguments")
  void testGetMachineType(String stubbedMockMachineType, String expectedMockMachineType) {
    TestUtils.stubEndpoint("/instance/machine-type", stubbedMockMachineType);
    assertEquals(expectedMockMachineType, mockMetadataConfig.getMachineType());
  }

  private static Stream<Arguments> provideMachineTypeRetrievalArguments() {
    return Stream.of(
        Arguments.of(mockInstanceType, mockInstanceType),
        Arguments.of("projects/640212054955/machineTypes/e2-medium", "e2-medium"),
        Arguments.of("", null),
        Arguments.of(null, null));
  }

  @Test
  void testGetInstanceId() {
    assertEquals(mockInstanceId, mockMetadataConfig.getInstanceId());
  }

  @Test
  void testGetClusterName() {
    assertEquals(mockClusterName, mockMetadataConfig.getClusterName());
  }

  @Test
  void testGetClusterLocation() {
    assertEquals(mockClusterLocation, mockMetadataConfig.getClusterLocation());
  }

  @Test
  void testGetInstanceHostName() {
    assertEquals(mockHostname, mockMetadataConfig.getInstanceHostName());
  }

  @Test
  void testGetInstanceName() {
    assertEquals(mockInstanceName, mockMetadataConfig.getInstanceName());
  }
}
