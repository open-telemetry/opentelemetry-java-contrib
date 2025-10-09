/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.management.RuntimeMBeanException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

class RecordingConfigurationTest {

  FlightRecorderConnection flightRecorderConnection = null;

  @BeforeEach
  void setup() {
    RecordingTest.deleteJfrFiles();
    flightRecorderConnection = RecordingTest.getFlightRecorderConnection();
  }

  @AfterEach
  void tearDown() {
    RecordingTest.deleteJfrFiles();
  }

  @Test
  void nullConfigThrows() {
    assertThatThrownBy(() -> new JfcFileConfiguration(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void brokenJfcConfigFileThrowsError() {
    assertThatThrownBy(() -> executeRecording("brokenJfcFile.jfc"))
        .isInstanceOf(RuntimeMBeanException.class);
  }

  @Test
  void jfcFileFromInputStreamCanBeRead() {
    IItemCollection recordingContent = executeRecording("sampleJfcFile.jfc");
    assertThat(containsEvent(recordingContent, "jdk.ThreadAllocationStatistics")).isTrue();
  }

  @Test
  void mapConfiguration() {

    Map<String, String> recordingConfigAsMap = new HashMap<>();
    recordingConfigAsMap.put("jdk.ObjectAllocationInNewTLAB#enabled", "true");
    recordingConfigAsMap.put("jdk.ObjectAllocationOutsideTLAB#enabled", "true");

    RecordingConfiguration recordingConfiguration = new MapConfiguration(recordingConfigAsMap);

    IItemCollection recordingContent = excecuteRecordingWithConfig(recordingConfiguration);
    assertThat(recordingContent).isNotNull();
    assertThat(containsEvent(recordingContent, "jdk.ObjectAllocationInNewTLAB")).isTrue();
    assertThat(containsEvent(recordingContent, "jdk.ObjectAllocationOutsideTLAB")).isTrue();
  }

  private static boolean containsEvent(IItemCollection recordingContent, String eventName) {
    for (IItemIterable iItemIterable : recordingContent) {
      for (IItem iItem : iItemIterable) {
        String currentEvent = iItem.getType().getIdentifier();
        if (currentEvent.equals(eventName)) {
          return true;
        }
      }
    }
    return false;
  }

  private IItemCollection executeRecording(String configFile) {
    JfcFileConfiguration configuration =
        new JfcFileConfiguration(
            RecordingConfigurationTest.class.getClassLoader().getResourceAsStream(configFile));
    return excecuteRecordingWithConfig(configuration);
  }

  private IItemCollection excecuteRecordingWithConfig(RecordingConfiguration configuration) {

    Path dumpFile = Paths.get(System.getProperty("user.dir"), "testRecordingDump_dumped.jfr");
    try {
      Files.deleteIfExists(dumpFile);
    } catch (IOException ioException) {
      fail("Precondition failed: Could not delete " + dumpFile.getFileName(), ioException);
    }

    try (Recording recording = flightRecorderConnection.newRecording(null, configuration)) {
      recording.start();
      Instant now = Instant.now();
      Instant then = now.plusSeconds(1);
      while (Instant.now().compareTo(then) < 0) {
        RecordingTest.fib(Short.MAX_VALUE); // do something
      }
      recording.stop();
      recording.dump(dumpFile.toString());
      assertThat(dumpFile).exists();

      try {
        return JfrLoaderToolkit.loadEvents(dumpFile.toFile());
      } catch (CouldNotLoadRecordingException e) {
        fail("Unable to load JFR data: ", e);
      }

    } catch (IllegalArgumentException badData) {
      fail("Issue in test data: " + badData.getMessage());
    } catch (IOException ioe) {
      // possible that this can be thrown, but should not happen in this context
      fail("IOException not expected: ", ioe);
    } catch (JfrConnectionException badBean) {
      fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
    } finally {
      try {
        Files.deleteIfExists(dumpFile);
      } catch (IOException ignore) {
        // Don't fail the test if we can't delete the file after the test is done.
      }
    }
    return null;
  }
}
