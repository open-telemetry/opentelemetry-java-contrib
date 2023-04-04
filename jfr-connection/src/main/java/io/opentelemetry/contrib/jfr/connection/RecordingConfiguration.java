/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A flight recorder configuration controls the amount of data that is collected.
 *
 * @see FlightRecorderConnection#newRecording(RecordingOptions, RecordingConfiguration)
 * @see FlightRecorderConnection#startRecording(RecordingOptions, RecordingConfiguration)
 */
public interface RecordingConfiguration {

  /**
   * Convenience for selecting the pre-defined 'default' configuration that is standard with the
   * JDK. The default configuration is suitable for continuous recordings.
   */
  public static final RecordingConfiguration DEFAULT_CONFIGURATION =
      new PredefinedConfiguration("default");

  /**
   * Convenience for referencing the 'profile' configuration that is standard with the JDK. The
   * profile configuration collects more events and is suitable for profiling an application.
   */
  public static final RecordingConfiguration PROFILE_CONFIGURATION =
      new PredefinedConfiguration("profile");

  /**
   * Call the appropriate method on the FlightRecorderMXBean to set the configuration.
   *
   * @param id The id of the recording to set the configuration on.
   * @param mBeanServerConnection The connection to the FlightRecorderMXBean
   * @param objectName The object name of the FlightRecorderMXBean
   * @throws IOException If an I/O error occurs
   * @throws JfrConnectionException If the invoke call throws an exception
   */
  void invokeSetConfiguration(
      long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
      throws IOException, JfrConnectionException;
}
