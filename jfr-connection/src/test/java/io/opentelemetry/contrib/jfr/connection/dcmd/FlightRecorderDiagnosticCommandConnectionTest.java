/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection.dcmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.jfr.connection.JfrStreamingException;
import io.opentelemetry.contrib.jfr.connection.RecordingConfiguration;
import io.opentelemetry.contrib.jfr.connection.RecordingOptions;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class FlightRecorderDiagnosticCommandConnectionTest {

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
        JfrStreamingException.class,
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
