/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.errorprone.annotations.Keep;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.stream.Stream;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class FlightRecorderDiagnosticCommandConnectionTest {

  @Keep
  static Stream<Arguments> assertJdkHasUnlockCommercialFeatures() {
    return Stream.of(
        Arguments.of("Oracle Corporation", "1.8.0_401", true),
        Arguments.of("AdoptOpenJDK", "1.8.0_282", false),
        Arguments.of("Oracle Corporation", "10.0.2", true),
        Arguments.of("Oracle Corporation", "9.0.4", true),
        Arguments.of("Oracle Corporation", "11.0.22", false),
        Arguments.of("Microsoft", "11.0.13", false),
        Arguments.of("Microsoft", "17.0.3", false),
        Arguments.of("Oracle Corporation", "21.0.3", false));
  }

  @ParameterizedTest
  @MethodSource
  void assertJdkHasUnlockCommercialFeatures(String vmVendor, String vmVersion, boolean expected)
      throws Exception {

    MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);

    try (MockedStatic<ManagementFactory> mockedStatic = mockStatic(ManagementFactory.class)) {
      mockedStatic
          .when(
              () -> ManagementFactory.getPlatformMXBean(mBeanServerConnection, RuntimeMXBean.class))
          .thenAnswer(
              new Answer<RuntimeMXBean>() {
                @Override
                public RuntimeMXBean answer(InvocationOnMock invocation) {
                  RuntimeMXBean mockedRuntimeMxBean = mock(RuntimeMXBean.class);
                  when(mockedRuntimeMxBean.getVmVendor()).thenReturn(vmVendor);
                  when(mockedRuntimeMxBean.getVmVersion()).thenReturn(vmVersion);
                  return mockedRuntimeMxBean;
                }
              });

      boolean actual =
          FlightRecorderDiagnosticCommandConnection.jdkHasUnlockCommercialFeatures(
              mBeanServerConnection);
      assertEquals(expected, actual, "Expected " + expected + " for " + vmVendor + " " + vmVersion);
    }
  }

  @Test
  void assertCommercialFeaturesUnlocked() throws Exception {
    ObjectName objectName = mock(ObjectName.class);
    MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "unlocked");
    FlightRecorderDiagnosticCommandConnection.assertCommercialFeaturesUnlocked(
        mBeanServerConnection, objectName);
  }

  @Test
  void assertCommercialFeaturesLockedThrows() throws Exception {
    assertThrows(
        JfrConnectionException.class,
        () -> {
          ObjectName objectName = mock(ObjectName.class);
          MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "locked");
          FlightRecorderDiagnosticCommandConnection.assertCommercialFeaturesUnlocked(
              mBeanServerConnection, objectName);
        });
  }

  @Test
  void closeRecording() throws Exception {
    assertThrows(UnsupportedOperationException.class, () -> createconnection().closeRecording(1));
  }

  @Test
  void testGetStream() throws Exception {
    assertThrows(
        UnsupportedOperationException.class,
        () -> createconnection().getStream(1L, null, null, 0L));
  }

  @Test
  void testCloneRecording() throws Exception {
    assertThrows(
        UnsupportedOperationException.class, () -> createconnection().cloneRecording(1, false));
  }

  @Test
  void startRecordingParsesIdCorrectly() throws Exception {
    ObjectName objectName = mock(ObjectName.class);
    MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "unlocked");
    when(mBeanServerConnection.invoke(
            any(ObjectName.class), anyString(), any(Object[].class), any(String[].class)))
        .thenReturn("Started recording 99. ");
    FlightRecorderDiagnosticCommandConnection connection =
        new FlightRecorderDiagnosticCommandConnection(mBeanServerConnection, objectName);
    long id =
        connection.startRecording(
            new RecordingOptions.Builder().build(), RecordingConfiguration.PROFILE_CONFIGURATION);
    assertEquals(id, 99);
  }

  MBeanServerConnection mockMbeanServer(
      ObjectName objectName, String vmCheckCommercialFeaturesResponse) throws Exception {
    MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
    when(mBeanServerConnection.invoke(objectName, "vmCheckCommercialFeatures", null, null))
        .thenReturn(vmCheckCommercialFeaturesResponse);
    return mBeanServerConnection;
  }

  private FlightRecorderDiagnosticCommandConnection createconnection() throws Exception {
    ObjectName objectName = mock(ObjectName.class);
    MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "locked");
    return new FlightRecorderDiagnosticCommandConnection(mBeanServerConnection, objectName);
  }
}
