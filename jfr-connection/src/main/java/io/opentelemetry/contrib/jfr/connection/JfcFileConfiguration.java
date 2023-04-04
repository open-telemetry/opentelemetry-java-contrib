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
import java.util.stream.Collectors;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/** A {@code RecordingConfiguration} that is read from a jfc file. */
public class JfcFileConfiguration implements RecordingConfiguration {

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
  public void invokeSetConfiguration(
      long id, MBeanServerConnection mBeanServerConnection, ObjectName objectName)
      throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id, configuration};
      String[] argTypes = new String[] {long.class.getName(), String.class.getName()};
      mBeanServerConnection.invoke(objectName, "setConfiguration", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(
          getClass(), "invokeSetConfiguration", e);
    }
  }

  @Override
  public String toString() {
    return configuration;
  }
}
