/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/** A {@code RecordingConfiguration} defined from a map. */
public class MapConfiguration implements RecordingConfiguration {

  private final Map<String, String> configuration;

  /**
   * Sets a configuration from a Map
   *
   * @param configuration A map defining the JFR events to register. For example:
   *     {jdk.ObjectAllocationInNewTLAB#enabled=true, jdk.ObjectAllocationOutsideTLAB#enabled=true}
   */
  public MapConfiguration(Map<String, String> configuration) {
    this.configuration = configuration;
  }

  @Override
  public void invokeSetConfiguration(
      long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
      throws IOException, JfrConnectionException {
    if (!configuration.isEmpty()) {
      try {
        TabularData configAsTabular = OpenDataUtils.makeOpenData(configuration);
        Object[] args = new Object[] {id, configAsTabular};
        String[] argTypes = new String[] {long.class.getName(), TabularData.class.getName()};
        mBeanServerConnection.invoke(objectName, "setRecordingSettings", args, argTypes);
      } catch (OpenDataException
          | InstanceNotFoundException
          | MBeanException
          | ReflectionException e) {
        throw FlightRecorderConnection.canonicalJfrConnectionException(
            getClass(), "invokeSetConfiguration", e);
      }
    }
  }

  @Override
  public String toString() {
    return configuration.toString();
  }
}
