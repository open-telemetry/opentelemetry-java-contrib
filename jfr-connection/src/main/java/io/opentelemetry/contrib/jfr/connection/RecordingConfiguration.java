/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/** A flight recorder configuration controls the amount of data that is collected. */
public abstract class RecordingConfiguration {

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
   * A pre-defined configuration is one which you could select with the 'settings' option of the JVM
   * option 'StartFlightRecording', for example {@code
   * -XX:StartFlightRecording:settings=default.jfc}.
   */
  public static class PredefinedConfiguration extends RecordingConfiguration {
    private final String configurationName;

    @Override
    void invokeSetConfiguration(
        long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
        throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
      invokeSetConfiguration(
          id, mBeanServerConnection, objectName, configurationName, "setPredefinedConfiguration");
    }

    /**
     * Sets a pre-defined configuration to use with a {@code Recording}.
     *
     * @param configurationName The name of the pre-defined configuration, not {@code null}.
     * @throws NullPointerException if predefinedConfiguration is {@code null}
     */
    public PredefinedConfiguration(String configurationName) {
      this.configurationName = configurationName;
    }

    @Override
    public String toString() {
      return configurationName;
    }
  }

  /** A configuration that is read from a jfc file */
  public static class JfcFileConfiguration extends RecordingConfiguration {

    private final String configuration;

    /**
     * Sets a configuration from a jfc file to use with a {@code Recording}.
     *
     * @param configurationFile An InputStream containing the configuration file, not {@code null}.
     * @throws NullPointerException if predefinedConfiguration is {@code null}
     */
    public JfcFileConfiguration(InputStream configurationFile) {
      this.configuration = readConfigurationFile(configurationFile);
    }

    private static String readConfigurationFile(InputStream inputStream) {
      if (inputStream != null) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining());
      } else {
        throw new IllegalArgumentException("Null configuration provided");
      }
    }

    @Override
    void invokeSetConfiguration(
        long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
        throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
      invokeSetConfiguration(
          id, mBeanServerConnection, objectName, configuration, "setConfiguration");
    }

    @Override
    public String toString() {
      return configuration;
    }
  }

  /** A configuration defined from a map. */
  public static class MapConfiguration extends RecordingConfiguration {

    private final Map<String, String> configuration;

    /**
     * Sets a configuration from a Map
     *
     * @param configuration A map defining the JFR events to register. For example:
     *     {jdk.ObjectAllocationInNewTLAB#enabled=true,
     *     jdk.ObjectAllocationOutsideTLAB#enabled=true}
     */
    public MapConfiguration(Map<String, String> configuration) {
      this.configuration = configuration;
    }

    @Override
    void invokeSetConfiguration(
        long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
        throws InstanceNotFoundException, MBeanException, ReflectionException, IOException,
            OpenDataException {
      if (!configuration.isEmpty()) {
        TabularData configAsTabular = OpenDataUtils.makeOpenData(configuration);
        Object[] args = new Object[] {id, configAsTabular};
        String[] argTypes = new String[] {long.class.getName(), TabularData.class.getName()};
        mBeanServerConnection.invoke(objectName, "setRecordingSettings", args, argTypes);
      }
    }

    @Override
    public String toString() {
      return configuration.toString();
    }
  }

  abstract void invokeSetConfiguration(
      long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
      throws InstanceNotFoundException, MBeanException, ReflectionException, IOException,
          OpenDataException;

  static void invokeSetConfiguration(
      long id,
      MBeanServerConnection mBeanServerConnection,
      ObjectName objectName,
      String configurationName,
      String getMbeanSetterFunction)
      throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    if (configurationName.trim().length() > 0) {
      Object[] args = new Object[] {id, configurationName};
      String[] argTypes = new String[] {long.class.getName(), String.class.getName()};
      mBeanServerConnection.invoke(objectName, getMbeanSetterFunction, args, argTypes);
    }
  }
}
