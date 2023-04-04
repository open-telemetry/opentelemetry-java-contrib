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
import javax.annotation.Nonnull;
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
 * Represents a connection to a {@code jdk.management.jfr.FlightRecorderMXBean} of a JVM. {@code
 * FlightRecorderConnection} provides {@link #newRecording(RecordingOptions, RecordingConfiguration)
 * API} to create Java flight {@link Recording recordings}. More than one {@code Recording} can be
 * created.
 *
 * <p>To use this class, a {@code javax.management.MBeanServerConnection} is needed. This class uses
 * the connection to make calls to the MBean server and does not change the state of the connection.
 * Management of the connection is the concern of the caller and use of a {@code
 * FlightRecorderConnection} for an MBean server connection that is no longer valid will result in
 * {@code IOException} being thrown.
 *
 * <p>The {@code MBeanServerConnection} can be a connection to any MBean server. Typically, the
 * connection is to the platform MBean server obtained by calling {@code
 * java.lang.management.ManagementFactory.getPlatformMBeanServer()}. The connection can also be to a
 * remote MBean server via {@code javax.management.remote.JMXConnector}. Refer to the summary in the
 * javadoc of the {@code javax.management} package and of the {@code javax.management.remote}
 * package for details.
 */
public class FlightRecorderConnection {

  private static final String JFR_OBJECT_NAME = "jdk.management.jfr:type=FlightRecorder";

  /**
   * Create a connection to the {@code FlightRecorder} via JMX. This method either returns a {@code
   * FlightRecorderConnection}, or throws an exception. An {@code IOException} indicates a problem
   * with the connection to the MBean server. An {@code InstanceNotFoundException} indicates that
   * the FlightRecorder MBean is not registered on the target JVM. This could happen if the target
   * JVM does not support Java Flight Recorder, or if experimental features need to be enabled on
   * the target JVM. If an {@code InstanceNotFoundException} is thrown by a Java 8 JVM, consider
   * using {@link
   * io.opentelemetry.contrib.jfr.connection.dcmd.FlightRecorderDiagnosticCommandConnection}.
   *
   * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
   * @return A {@code FlightRecorderConnection}.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.MalformedObjectNameException}
   *     and indicates a bug in this class.
   * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
   */
  public static FlightRecorderConnection connect(MBeanServerConnection mBeanServerConnection)
      throws IOException, JfrConnectionException {
    Objects.requireNonNull(mBeanServerConnection);
    try {
      ObjectName objectName = new ObjectName(JFR_OBJECT_NAME);
      ObjectInstance objectInstance = mBeanServerConnection.getObjectInstance(objectName);
      return new FlightRecorderConnection(mBeanServerConnection, objectInstance.getObjectName());
    } catch (MalformedObjectNameException e) {
      // Not expected to happen. This exception comes from the ObjectName constructor. If
      // JFR_OBJECT_NAME is malformed, then this is an internal bug.
      throw new JfrConnectionException(JFR_OBJECT_NAME, e);
    } catch (InstanceNotFoundException e) {
      throw canonicalJfrConnectionException(FlightRecorderConnection.class, "connect", e);
    }
  }

  /**
   * Create a {@link Recording} with the given options and configuration. The {@code Recording} is
   * created in the {@link Recording.State#NEW} state. The recording will use the default values of
   * {@code jdk.management.jfr.FlightRecorderMXBean} for a parameter passed as {@code null}.
   *
   * @param recordingOptions The options to be used for the recording, or {@code null} for defaults.
   * @param recordingConfiguration The configuration to be used for the recording, or {@code null}
   *     for defaults.
   * @return A {@link Recording} object associated with this {@code FlightRecorderConnection}.
   */
  public Recording newRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) {
    return new Recording(this, recordingOptions, recordingConfiguration);
  }

  /**
   * Start a recording. This method creates a new recording, sets the configuration, and then starts
   * the recording. This method is called from the {@link Recording#start()} method.
   *
   * @param recordingOptions The {@code RecordingOptions} which was passed to the {@link
   *     #newRecording(RecordingOptions, RecordingConfiguration)} method. {@code null} is allowed.
   * @param recordingConfiguration The {@code RecordingConfiguration} which was passed to the {@link
   *     #newRecording(RecordingOptions, RecordingConfiguration)} method. {@code null} is allowed.
   * @return The id of the recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM. The cause may also be a
   *     {@code javax.management.openmbean.OpenDataException} which indicates a bug in the code of
   *     this class.
   */
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
      throw canonicalJfrConnectionException(getClass(), "startRecording", e);
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
        throw canonicalJfrConnectionException(getClass(), "setOptions", e);
      }
    }
  }

  private void setConfiguration(RecordingConfiguration recordingConfiguration, long id)
      throws IOException, JfrConnectionException {
    recordingConfiguration.invokeSetConfiguration(id, mBeanServerConnection, objectName);
  }

  /**
   * Stop a recording. This method is called from the {@link Recording#stop()} method.
   *
   * @param id The id of the recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  public void stopRecording(long id) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id};
      String[] argTypes = new String[] {long.class.getName()};
      mBeanServerConnection.invoke(objectName, "stopRecording", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw canonicalJfrConnectionException(getClass(), "stopRecording", e);
    }
  }

  /**
   * Writes recording data to the specified file. The recording must be started, but not necessarily
   * stopped. The {@code outputFile} argument is relevant to the machine where the JVM is running.
   *
   * @param id The id of the recording.
   * @param outputFile the system-dependent file name where data is written, not {@code null}
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   */
  public void dumpRecording(long id, String outputFile) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id, outputFile};
      String[] argTypes = new String[] {long.class.getName(), String.class.getName()};
      mBeanServerConnection.invoke(objectName, "copyTo", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw canonicalJfrConnectionException(getClass(), "dumpRecording", e);
    }
  }

  /**
   * Creates a copy of an existing recording, useful for extracting parts of a recording. The cloned
   * recording contains the same recording data as the original, but it has a new ID. If the
   * original recording is running, then the clone is also running.
   *
   * @param id The id of the recording being cloned.
   * @param stop Whether to stop the cloned recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   * @return id of the recording
   */
  public long cloneRecording(long id, boolean stop) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id, stop};
      String[] argTypes = new String[] {long.class.getName(), boolean.class.getName()};
      return (long) mBeanServerConnection.invoke(objectName, "cloneRecording", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw canonicalJfrConnectionException(getClass(), "cloneRecording", e);
    }
  }

  /**
   * Get the Java Flight Recording as an {@code java.io.InputStream}. This method is called from the
   * {@link Recording#getStream(Instant, Instant, long)} method.
   *
   * <p>The recording may contain data outside the {@code startTime} and {@code endTime} parameters.
   * Either or both of {@code startTime} and {@code endTime} may be {@code null}, in which case the
   * {@code FlightRecorderMXBean} will use a default value indicating the beginning and the end of
   * the recording, respectively.
   *
   * <p>The {@code blockSize} parameter specifies the number of bytes to read with a call to the
   * {@code FlightRecorderMXBean#readStream(long)} method. Setting blockSize to a very high value
   * may result in an OutOfMemoryError or an IllegalArgumentException, if the JVM deems the value
   * too large to handle.
   *
   * @param id The id of the recording.
   * @param startTime The point in time to start the recording stream, possibly {@code null}.
   * @param endTime The point in time to end the recording stream, possibly {@code null}.
   * @param blockSize The number of bytes to read at a time.
   * @return A {@code InputStream} of the Java Flight Recording data.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM. The cause may also be a
   *     {@code javax.management.openmbean.OpenDataException} which indicates a bug in the code of
   *     this class.
   */
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
      throw canonicalJfrConnectionException(getClass(), "getStream", e);
    }
  }

  /**
   * Close the recording. This method is called from the {@link Recording#close()} method.
   *
   * @param id The id of the recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  public void closeRecording(long id) throws IOException, JfrConnectionException {
    try {
      Object[] args = new Object[] {id};
      String[] argTypes = new String[] {long.class.getName()};
      mBeanServerConnection.invoke(objectName, "closeRecording", args, argTypes);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw canonicalJfrConnectionException(getClass(), "closeRecording", e);
    }
  }

  /**
   * Convenience method to create a JfrConnectionException with a canonical message and a cause.
   *
   * @param clazz The class that caught the {@code cause}.
   * @param methodName The name of the method that caught the {@code cause}.
   * @param cause The exception that was caught.
   * @return A JfrConnectionException with a canonical message.
   */
  protected static JfrConnectionException canonicalJfrConnectionException(
      @Nonnull Class<?> clazz, @Nonnull String methodName, @Nonnull Exception cause) {
    String canonicalMessage =
        String.format("%1s.%2s caught: %3s", clazz.getSimpleName(), methodName, cause.getMessage());
    return new JfrConnectionException(canonicalMessage, cause);
  }

  /**
   * Constructor is called from the static {@link
   * FlightRecorderConnection#connect(MBeanServerConnection)} method, and from the {@link
   * io.opentelemetry.contrib.jfr.connection.dcmd.FlightRecorderDiagnosticCommandConnection#connect(MBeanServerConnection)}
   * method. It is not possible to create a FlightRecorderConnection directly.
   *
   * @param mBeanServerConnection The connection to the MBeanServer.
   * @param objectName The name of the MBean we are connected to.
   */
  protected FlightRecorderConnection(
      MBeanServerConnection mBeanServerConnection, ObjectName objectName) {
    this.mBeanServerConnection = mBeanServerConnection;
    this.objectName = objectName;
  }

  /** The MBeanServerConnection. */
  protected final MBeanServerConnection mBeanServerConnection;
  /** The ObjectName of the MBean we are connecting to. */
  protected final ObjectName objectName;
}
