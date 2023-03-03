/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection.dcmd;

import io.opentelemetry.contrib.jfr.connection.FlightRecorderConnection;
import io.opentelemetry.contrib.jfr.connection.JfrStreamingException;
import io.opentelemetry.contrib.jfr.connection.Recording;
import io.opentelemetry.contrib.jfr.connection.RecordingConfiguration;
import io.opentelemetry.contrib.jfr.connection.RecordingOptions;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Represents a connection to the {@code com.sun.management DiagnosticCommand} MBean of a JVM. This
 * is the same mechanism used by the Java <i>jcmd</i> tool. {@code
 * FlightRecorderDiagnosticCommandConnection} provides {@link #newRecording(RecordingOptions,
 * RecordingConfiguration) API} to create Java flight {@link Recording recordings}. More than one
 * {@code Recording} can be created.
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
public class FlightRecorderDiagnosticCommandConnection extends FlightRecorderConnection {
  private static final String DIAGNOSTIC_COMMAND_OBJECT_NAME =
      "com.sun.management:type=DiagnosticCommand";
  private static final String JFR_START_REGEX = "Started recording (.+?)\\. .*";
  private static final Pattern JFR_START_PATTERN = Pattern.compile(JFR_START_REGEX, Pattern.DOTALL);

  // All JFR commands take String[] parameters
  private static final String[] signature = new String[] {"[Ljava.lang.String;"};

  /**
   * Create a connection to the {@code FlightRecorder} via JMX. This method either returns a {@code
   * FlightRecorderConnection}, or throws an exception. An {@code IOException} indicates a problem
   * with the connection to the MBean server. An {@code InstanceNotFoundException} indicates that
   * the FlightRecorder MBean is not registered on the target JVM. This could happen if the target
   * JVM does not support Java Flight Recorder, or if expermental features need to be enabled on the
   * target JVM.
   *
   * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
   * @return A {@code FlightRecorderConnection}.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws InstanceNotFoundException The FlightRecorder MBean is not registered on the target JVM.
   * @throws JfrStreamingException Wraps a {@code javax.management.MalformedObjectNameException} and
   *     indicates a bug in this class.
   * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
   */
  public static FlightRecorderConnection connect(MBeanServerConnection mBeanServerConnection)
      throws IOException, InstanceNotFoundException, JfrStreamingException {
    Objects.requireNonNull(mBeanServerConnection);
    try {
      ObjectInstance objectInstance =
          mBeanServerConnection.getObjectInstance(new ObjectName(DIAGNOSTIC_COMMAND_OBJECT_NAME));
      ObjectName objectName = objectInstance.getObjectName();
      assertCommercialFeaturesUnlocked(mBeanServerConnection, objectName);
      return new FlightRecorderDiagnosticCommandConnection(
          mBeanServerConnection, objectInstance.getObjectName());
    } catch (MalformedObjectNameException e) {
      // Not expected to happen. This exception comes from the ObjectName constructor. If
      // DIAGNOSTIC_COMMAND_OBJECT_NAME is malformed, then this is an internal bug.
      throw new JfrStreamingException(DIAGNOSTIC_COMMAND_OBJECT_NAME, e);
    }
  }

  /* package scope for testing */ FlightRecorderDiagnosticCommandConnection(
      MBeanServerConnection mBeanServerConnection, ObjectName objectName) {
    super(mBeanServerConnection, objectName);
  }

  /**
   * Start a recording. This method creates a new recording, sets the configuration, and then starts
   * the recording. This method is called from the {@link Recording#start()} method.
   *
   * @param recordingOptions The {@code RecordingOptions} which was passed to the {@link
   *     #newRecording(RecordingOptions, RecordingConfiguration)} method
   * @param recordingConfiguration The {@code RecordingConfiguration} which was passed to the {@link
   *     #newRecording(RecordingOptions, RecordingConfiguration)} method
   * @return The id of the recording.
   * @throws JfrStreamingException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM. The cause may also be a
   *     {@code javax.management.openmbean.OpenDataException} which indicates a bug in the code of
   *     this class.
   */
  @Override
  public long startRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration)
      throws JfrStreamingException {

    if (recordingConfiguration instanceof RecordingConfiguration.MapConfiguration) {
      throw new JfrStreamingException(
          "Map configuration not available for "
              + FlightRecorderDiagnosticCommandConnection.class.getSimpleName()
              + ".");
    }

    Object[] params = formOptions(recordingOptions, recordingConfiguration);

    // jfrStart returns "Started recording 2." and some more stuff, but all we care about is the
    // name of the recording.
    try {
      String jfrStart =
          (String) mBeanServerConnection.invoke(objectName, "jfrStart", params, signature);
      String name;
      Matcher matcher = JFR_START_PATTERN.matcher(jfrStart);
      if (matcher.find()) {
        name = matcher.group(1);
        return Long.parseLong(name);
      }
    } catch (InstanceNotFoundException | IOException | ReflectionException | MBeanException e) {
      throw new JfrStreamingException("Failed to start recording", e);
    }
    throw new JfrStreamingException("Failed to start recording");
  }

  private static Object[] formOptions(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration)
      throws JfrStreamingException {
    List<String> options =
        recordingOptions.getRecordingOptions().entrySet().stream()
            .filter(kv -> !kv.getKey().equals("disk")) // not supported on Java 8
            .map(kv -> kv.getKey() + "=" + kv.getValue())
            .collect(Collectors.toList());

    if (!(recordingConfiguration instanceof RecordingConfiguration.PredefinedConfiguration)) {
      throw new JfrStreamingException(
          "Java 8 currently only supports predefined configurations (default/profile)");
    }

    List<String> settings = Collections.singletonList("settings=" + recordingConfiguration);

    List<String> params = new ArrayList<>();
    params.addAll(settings);
    params.addAll(options);
    return mkParamsArray(params);
  }

  /**
   * Stop a recording. This method is called from the {@link Recording#stop()} method.
   *
   * @param id The id of the recording.
   * @throws JfrStreamingException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  @Override
  public void stopRecording(long id) throws JfrStreamingException {
    try {
      Object[] params = mkParams("name=" + id);
      mBeanServerConnection.invoke(objectName, "jfrStop", params, signature);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
      throw new JfrStreamingException("Failed to stop recording", e);
    }
  }

  /**
   * Writes recording data to the specified file. The recording must be started, but not necessarily
   * stopped. The {@code outputFile} argument is relevant to the machine where the JVM is running.
   *
   * @param id The id of the recording.
   * @param outputFile the system-dependent file name where data is written, not {@code null}
   * @throws JfrStreamingException Wraps a {@code javax.management.JMException}.
   */
  @Override
  public void dumpRecording(long id, String outputFile) throws JfrStreamingException {
    try {
      Object[] params = mkParams("filename=" + outputFile, "recording=" + id, "compress=true");
      mBeanServerConnection.invoke(objectName, "jfrDump", params, signature);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
      throw new JfrStreamingException("Failed to dump recording", e);
    }
  }

  /**
   * Not supported on Java 8
   *
   * @param id The id of the recording being cloned.
   * @param stop Whether to stop the cloned recording.
   * @return id of the recording
   */
  @Override
  public long cloneRecording(long id, boolean stop) {
    throw new UnsupportedOperationException("Clone not supported on Java 8");
  }

  /** Not supported on Java 8 */
  @Override
  public InputStream getStream(long id, Instant startTime, Instant endTime, long blockSize) {
    throw new UnsupportedOperationException("getStream not supported on Java 8");
  }

  /** Not supported on Java 8 */
  @Override
  public void closeRecording(long id) {
    throw new UnsupportedOperationException("closeRecording not supported on Java 8");
  }

  // visible for testing
  static void assertCommercialFeaturesUnlocked(
      MBeanServerConnection mBeanServerConnection, ObjectName objectName)
      throws JfrStreamingException {
    try {
      Object unlockedMessage =
          mBeanServerConnection.invoke(objectName, "vmCheckCommercialFeatures", null, null);
      if (unlockedMessage instanceof String) {
        boolean unlocked = ((String) unlockedMessage).contains("unlocked");
        if (!unlocked) {
          throw new JfrStreamingException(
              "Unlocking commercial features may be required. This must be explicitly enabled by adding -XX:+UnlockCommercialFeatures");
        }
      }
    } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
      throw new JfrStreamingException("Unable to determine if commercial features are unlocked", e);
    }
  }

  private static Object[] mkParamsArray(List<String> args) {
    return new Object[] {args.toArray(new String[0])};
  }

  private static Object[] mkParams(String... args) {
    return new Object[] {args};
  }
}
