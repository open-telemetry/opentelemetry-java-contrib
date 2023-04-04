/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * A pre-defined {@code RecordingConfiguration} is one which you could select with the 'settings'
 * option of the JVM option 'StartFlightRecording', for example {@code
 * -XX:StartFlightRecording:settings=default.jfc}.
 */
public class PredefinedConfiguration implements RecordingConfiguration {
  private final String configurationName;

  @Override
  public void invokeSetConfiguration(
      long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
      throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id, configurationName};
      String[] argTypes = new String[] {long.class.getName(), String.class.getName()};
      mBeanServerConnection.invoke(objectName, "setPredefinedConfiguration", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw FlightRecorderConnection.canonicalJfrConnectionException(
          getClass(), "invokeSetConfiguration", e);
    }
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
