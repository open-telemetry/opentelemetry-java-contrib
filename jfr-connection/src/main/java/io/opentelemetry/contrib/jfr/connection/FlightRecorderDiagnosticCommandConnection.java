/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

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
 * This class is the implementation detail of the {@link FlightRecorderConnection} interface which
 * uses a connection to a {@code com.sun.management DiagnosticCommand} MBean of a JVM. Because it is
 * implementation detail, it is not part of the public API of this library.
 *
 * @see FlightRecorderConnection#diagnosticCommandConnection(MBeanServerConnection)
 */
final class FlightRecorderDiagnosticCommandConnection implements FlightRecorderConnection {
  private static final String DIAGNOSTIC_COMMAND_OBJECT_NAME =
      "com.sun.management:type=DiagnosticCommand";
  private static final String JFR_START_REGEX = "Started recording (.+?)\\. .*";
  private static final Pattern JFR_START_PATTERN = Pattern.compile(JFR_START_REGEX, Pattern.DOTALL);

  // All JFR commands take String[] parameters
  private static final String[] signature = new String[] {"[Ljava.lang.String;"};

  /**
   * Create a connection to the {@code DiagnosticCommand} MBean.
   *
   * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
   * @return A {@code FlightRecorderConnection}.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Indicates an issue with the DiagnosticCommand MBean in the JVM.
   * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
   * @see FlightRecorderConnection#diagnosticCommandConnection(MBeanServerConnection)
   */
  static FlightRecorderConnection connect(MBeanServerConnection mBeanServerConnection)
      throws IOException, JfrConnectionException {
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
      throw new JfrConnectionException(DIAGNOSTIC_COMMAND_OBJECT_NAME, e);
    } catch (InstanceNotFoundException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(
          FlightRecorderDiagnosticCommandConnection.class, "connect", e);
    }
  }

  /* package scope for testing */
  FlightRecorderDiagnosticCommandConnection(
      MBeanServerConnection mBeanServerConnection, ObjectName objectName) {
    this.mBeanServerConnection = mBeanServerConnection;
    this.objectName = objectName;
  }

  /** The MBeanServerConnection. */
  private final MBeanServerConnection mBeanServerConnection;

  /** The ObjectName of the MBean we are connecting to. */
  private final ObjectName objectName;

  /** {@inheritDoc} */
  @Override
  public Recording newRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) {
    return new Recording(this, recordingOptions, recordingConfiguration);
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
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws JfrConnectionException Indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  @Override
  public long startRecording(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration)
      throws IOException, JfrConnectionException {

    if (!(recordingConfiguration instanceof PredefinedConfiguration)) {
      throw JfrConnectionException.canonicalJfrConnectionException(
          getClass(),
          "startRecording",
          new UnsupportedOperationException(
              "Java 8 currently only supports predefined configurations (default/profile)"));
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
    } catch (InstanceNotFoundException | ReflectionException | MBeanException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "startRecording", e);
    }
    throw JfrConnectionException.canonicalJfrConnectionException(
        getClass(), "startRecording", new IllegalStateException("Failed to parse jfrStart output"));
  }

  private static Object[] formOptions(
      RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) {
    List<String> options =
        recordingOptions.getRecordingOptions().entrySet().stream()
            .filter(kv -> !kv.getKey().equals("disk")) // not supported on Java 8
            .map(kv -> kv.getKey() + "=" + kv.getValue())
            .collect(Collectors.toList());

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
   * @throws JfrConnectionException Wraps an {@code javax.management.InstanceNotFoundException}, a
   *     {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
   *     and indicates an issue with the FlightRecorderMXBean in the JVM.
   */
  @Override
  public void stopRecording(long id) throws JfrConnectionException {
    try {
      Object[] params = mkParams("name=" + id);
      mBeanServerConnection.invoke(objectName, "jfrStop", params, signature);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "stopRecording", e);
    }
  }

  /**
   * Writes recording data to the specified file. The recording must be started, but not necessarily
   * stopped. The {@code outputFile} argument is relevant to the machine where the JVM is running.
   *
   * @param id The id of the recording.
   * @param outputFile the system-dependent file name where data is written, not {@code null}
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   */
  @Override
  public void dumpRecording(long id, String outputFile) throws IOException, JfrConnectionException {
    try {
      Object[] params = mkParams("filename=" + outputFile, "recording=" + id, "compress=true");
      mBeanServerConnection.invoke(objectName, "jfrDump", params, signature);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(getClass(), "dumpRecording", e);
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
      throws IOException, JfrConnectionException {
    try {
      Object unlockedMessage =
          mBeanServerConnection.invoke(objectName, "vmCheckCommercialFeatures", null, null);
      if (unlockedMessage instanceof String) {
        boolean unlocked = ((String) unlockedMessage).contains("unlocked");
        if (!unlocked) {
          throw new UnsupportedOperationException(
              "Unlocking commercial features may be required. This must be explicitly enabled by adding -XX:+UnlockCommercialFeatures");
        }
      }
    } catch (InstanceNotFoundException
        | MBeanException
        | ReflectionException
        | UnsupportedOperationException e) {
      throw JfrConnectionException.canonicalJfrConnectionException(
          FlightRecorderDiagnosticCommandConnection.class, "assertCommercialFeaturesUnlocked", e);
    }
  }

  private static Object[] mkParamsArray(List<String> args) {
    return new Object[] {args.toArray(new String[0])};
  }

  private static Object[] mkParams(String... args) {
    return new Object[] {args};
  }
}
