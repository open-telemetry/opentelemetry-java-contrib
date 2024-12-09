/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class FlightRecorderDiagnosticCommandConnectionTest {

  @Test
  void assertCommercialFeaturesUnlocked() throws Exception {
    MBeanServer mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName("com.sun.management:type=DiagnosticCommand");
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

  @Test
  void endToEndTest() throws Exception {

    MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
    FlightRecorderConnection flightRecorderConnection =
        FlightRecorderDiagnosticCommandConnection.connect(mBeanServer);
    RecordingOptions recordingOptions =
        new RecordingOptions.Builder().disk("true").duration("5s").build();
    RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;
    Path tempFile = Files.createTempFile("recording", ".jfr");

    try (Recording recording =
        flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration)) {

      recording.start();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      recording.dump(tempFile.toString());
      recording.stop();
    } finally {
      if (!Files.exists(tempFile)) {
        fail("Recording file not found");
      }
      Files.deleteIfExists(tempFile);
    }
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
