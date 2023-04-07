/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import javax.management.MBeanServerConnection;

/**
 * Represents a connection to a {@code jdk.management.jfr.FlightRecorderMXBean} of a JVM. Use {@link
 * FlightRecorderConnection#connect(MBeanServerConnection)} to create a connection. For Java 8 JVMs
 * that may not have the {@code jdk.management.jfr.FlightRecorderMXBean} available, use {@link
 * FlightRecorderConnection#diagnosticCommandConnection(MBeanServerConnection)}. {@code
 * FlightRecorderConnection} provides {@link #newRecording(RecordingOptions, RecordingConfiguration)
 * API} to create Java flight {@link Recording recordings}. More than one {@code Recording} can be
 * created on a connection.
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
public interface FlightRecorderConnection {

  /**
   * Create a connection to the {@code FlightRecorderMXBean}. This method either returns a {@code
   * FlightRecorderConnection}, or throws an exception. An {@code IOException} indicates a problem
   * with the connection to the MBean server. A {@code JfrConnectionException} indicates some issue
   * with the MBean server on the target JVM. This could happen if the target JVM does not support
   * Java Flight Recorder, or if experimental features need to be enabled on the target JVM. On a
   * Java 8 JVM, consider using {@link
   * FlightRecorderConnection#diagnosticCommandConnection(MBeanServerConnection)} if the cause of a
   * {@code JfrConnectionException} is an {@code InstanceNotFoundException}.
   *
   * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
   * @return A {@code FlightRecorderConnection}.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.MalformedObjectNameException}
   *     and indicates a bug in this class.
   * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
   */
  static FlightRecorderConnection connect(MBeanServerConnection mBeanServerConnection)
      throws IOException, JfrConnectionException {
    return FlightRecorderMXBeanConnection.connect(mBeanServerConnection);
  }

  /**
   * Create a connection to the {@code com.sun.management DiagnosticCommand} MBean. This method
   * either returns a {@code FlightRecorderConnection}, or throws an exception. An {@code
   * IOException} indicates a problem with the connection to the MBean server. A {@code
   * JfrConnectionException} indicates some issue with the MBean server on the target JVM. This
   * could happen if the target JVM does not support Java Flight Recorder, or if experimental
   * features need to be enabled on the target JVM.
   *
   * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
   * @return A {@code FlightRecorderConnection}.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.MalformedObjectNameException}
   *     and indicates a bug in this class.
   * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
   */
  static FlightRecorderConnection diagnosticCommandConnection(
      MBeanServerConnection mBeanServerConnection) throws IOException, JfrConnectionException {
    return FlightRecorderDiagnosticCommandConnection.connect(mBeanServerConnection);
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
  Recording newRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration);

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
  long startRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration)
      throws IOException, JfrConnectionException;

  /**
   * Stop a recording. This method is called from the {@link Recording#stop()} method.
   *
   * @param id The id of the recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  void stopRecording(long id) throws IOException, JfrConnectionException;

  /**
   * Writes recording data to the specified file. The recording must be started, but not necessarily
   * stopped. The {@code outputFile} argument is relevant to the machine where the JVM is running.
   *
   * @param id The id of the recording.
   * @param outputFile the system-dependent file name where data is written, not {@code null}
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   */
  void dumpRecording(long id, String outputFile) throws IOException, JfrConnectionException;

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
  long cloneRecording(long id, boolean stop) throws IOException, JfrConnectionException;

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
  InputStream getStream(long id, Instant startTime, Instant endTime, long blockSize)
      throws IOException, JfrConnectionException;

  /**
   * Close the recording. This method is called from the {@link Recording#close()} method.
   *
   * @param id The id of the recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  void closeRecording(long id) throws IOException, JfrConnectionException;
}
