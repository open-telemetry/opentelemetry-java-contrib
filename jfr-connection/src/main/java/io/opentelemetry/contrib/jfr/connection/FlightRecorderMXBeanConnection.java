/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/**
 * This class is the implementation detail of the {@link FlightRecorderConnection} interface which
 * uses a connection to a {@code jdk.management.jfr.FlightRecorderMXBean} of a JVM. Because it is
 * implementation detail, it is not part of the public API of this library.
 *
 * @see FlightRecorderConnection#connectToFlightRecorderMXBean(MBeanServerConnection)
 */
/* package scope */ final class FlightRecorderMXBeanConnection implements FlightRecorderConnection {

  private static final String JFR_OBJECT_NAME = "jdk.management.jfr:type=FlightRecorder";

  /**
   * Create a connection to the {@code FlightRecorderMXBean}.
   *
   * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
   * @return A {@code FlightRecorderConnection}.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.MalformedObjectNameException}
   *     and indicates a bug in this class.
   * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
   * @see FlightRecorderConnection#connect(MBeanServerConnection)
   */
  /* package access */ static FlightRecorderConnection connect(
      MBeanServerConnection mBeanServerConnection) throws IOException, JfrConnectionException {
    Objects.requireNonNull(mBeanServerConnection);
    try {
      ObjectName objectName = new ObjectName(JFR_OBJECT_NAME);
      ObjectInstance objectInstance = mBeanServerConnection.getObjectInstance(objectName);
      return new FlightRecorderMXBeanConnection(
          mBeanServerConnection, objectInstance.getObjectName());
    } catch (MalformedObjectNameException e) {
      // Not expected to happen. This exception comes from the ObjectName constructor. If
      // JFR_OBJECT_NAME is malformed, then this is an internal bug.
      throw new JfrConnectionException(JFR_OBJECT_NAME, e);
    } catch (InstanceNotFoundException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(
          FlightRecorderMXBeanConnection.class, "connect", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Recording newRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) {
    return new Recording(this, recordingOptions, recordingConfiguration);
  }

  /** {@inheritDoc} */
  @Override
  public long startRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration)
      throws IOException, JfrConnectionException {

    try {
      Object[] args = new Object[] {};
      String[] argTypes = new String[] {};
      long id = (long) mBeanServerConnection.invoke(objectName, "newRecording", args, argTypes);

      if (recordingConfiguration != null) {
        setConfiguration(recordingConfiguration, id);
      }

      if (recordingOptions != null) {
        setOptions(recordingOptions, id);
      }

      args = new Object[] {id};
      argTypes = new String[] {long.class.getName()};
      mBeanServerConnection.invoke(objectName, "startRecording", args, argTypes);

      return id;
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "startRecording", e);
    }
  }

  private void setOptions(RecordingOptions recordingOptions, long id)
      throws IOException, JfrConnectionException {
    Map<String, String> options = recordingOptions.getRecordingOptions();
    if (options != null && !options.isEmpty()) {
      try {
        TabularData recordingOptionsParam = OpenDataUtils.makeOpenData(options);
        Object[] args = new Object[] {id, recordingOptionsParam};
        String[] argTypes = new String[] {long.class.getName(), TabularData.class.getName()};
        mBeanServerConnection.invoke(objectName, "setRecordingOptions", args, argTypes);
      } catch (OpenDataException
          | InstanceNotFoundException
          | MBeanException
          | ReflectionException e) {
        throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "setOptions", e);
      }
    }
  }

  private void setConfiguration(RecordingConfiguration recordingConfiguration, long id)
      throws IOException, JfrConnectionException {
    recordingConfiguration.invokeSetConfiguration(id, mBeanServerConnection, objectName);
  }

  /** {@inheritDoc} */
  @Override
  public void stopRecording(long id) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id};
      String[] argTypes = new String[] {long.class.getName()};
      mBeanServerConnection.invoke(objectName, "stopRecording", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "stopRecording", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void dumpRecording(long id, String outputFile) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id, outputFile};
      String[] argTypes = new String[] {long.class.getName(), String.class.getName()};
      mBeanServerConnection.invoke(objectName, "copyTo", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "dumpRecording", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public long cloneRecording(long id, boolean stop) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id, stop};
      String[] argTypes = new String[] {long.class.getName(), boolean.class.getName()};
      return (long) mBeanServerConnection.invoke(objectName, "cloneRecording", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "cloneRecording", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public InputStream getStream(long id, Instant startTime, Instant endTime, long blockSize)
      throws IOException, JfrConnectionException {
    Map<String, String> options = new HashMap<>();
    if (startTime != null) {
      options.put("startTime", startTime.toString());
    }
    if (endTime != null) {
      options.put("endTime", endTime.toString());
    }
    if (blockSize > 0) {
      options.put("blockSize", Long.toString(blockSize));
    }

    try {
      TabularData streamOptions = OpenDataUtils.makeOpenData(options);
      Object[] args = new Object[] {id, streamOptions};
      String[] argTypes = new String[] {long.class.getName(), TabularData.class.getName()};
      long streamId = (long) mBeanServerConnection.invoke(objectName, "openStream", args, argTypes);
      return new JfrStream(mBeanServerConnection, objectName, streamId);
    } catch (OpenDataException
        | InstanceNotFoundException
        | MBeanException
        | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "getStream", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void closeRecording(long id) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id};
      String[] argTypes = new String[] {long.class.getName()};
      mBeanServerConnection.invoke(objectName, "closeRecording", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "closeRecording", e);
    }
  }

  /**
   * Constructor is called from the static {@link
   * FlightRecorderMXBeanConnection#connect(MBeanServerConnection)} method It is not possible to
   * create a FlightRecorderMXBeanConnection directly.
   *
   * @param mBeanServerConnection The connection to the MBeanServer.
   * @param objectName The name of the MBean we are connected to.
   */
  private FlightRecorderMXBeanConnection(
      MBeanServerConnection mBeanServerConnection, ObjectName objectName) {
    this.mBeanServerConnection = mBeanServerConnection;
    this.objectName = objectName;
  }

  /** The MBeanServerConnection. */
  private final MBeanServerConnection mBeanServerConnection;
  /** The ObjectName of the MBean we are connecting to. */
  private final ObjectName objectName;
}
