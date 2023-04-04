/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * Provides a means to start, stop and dump recording data. To create a {@code Recording}, use
 * {@link FlightRecorderConnection#newRecording(RecordingOptions, RecordingConfiguration)}.
 *
 * @see <a
 *     href="https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jfr/jdk/jfr/Recording.html">jdk.jfr.Recording</a>
 */
public class Recording implements AutoCloseable {

  /**
   * A {@code Recording} may be in one of these states. Note that a {@code Recording} is no longer
   * usable once it is in the {@code CLOSED} state. Valid state transitions are:
   *
   * <ul>
   *   <li>{@code NEW -> [RECORDING, STOPPED, CLOSED]}
   *   <li>{@code RECORDING -> [RECORDING, STOPPED, CLOSED]}
   *   <li>{@code STOPPED -> [RECORDING, STOPPED, CLOSED]}
   *   <li>{@code CLOSED -> [CLOSED]}
   * </ul>
   *
   * Calling a method on {@code Recording} that would cause an invalid transition will raise an
   * IllegalStateException.
   */
  public enum State {
    /** The {@code Recording} has been created. */
    NEW,
    /** The {@code Recording} has been started. */
    RECORDING,
    /** The {@code Recording} has been stopped. */
    STOPPED,
    /**
     * The {@code Recording} has been closed. Once the recording has been closed, it is no longer
     * usable.
     */
    CLOSED;
  }

  // Format for IllegalStateException that this class might throw
  // {0} is the state the code is trying to transition to.
  // {1} are the states that the instance could be in for a valid transition.
  private static final MessageFormat illegalStateFormat =
      new MessageFormat("Recording state {0} not in [{1}]");

  /**
   * Helper for formatting the message for an IllegalStateException that may be thrown by methods of
   * this class.
   *
   * @param actual This is the state that the Recording is in currently
   * @param expected This is the state that the Recording should be in for a valid transition to
   *     occur
   * @param others Additional <em>expected</em> states
   * @return a consistently formatted message for an IllegalStateException that may be thrown by
   *     methods of this class.
   */
  private static String createIllegalStateExceptionMessage(
      @Nullable State actual, State expected, State... others) {
    String[] args = new String[] {actual != null ? actual.name() : "null", expected.name()};
    if (others != null) {
      for (State state : others) {
        args[1] = args[1].concat(", ").concat(state.name());
      }
    }
    String msg = illegalStateFormat.format(args);
    return msg;
  }

  private final FlightRecorderConnection connection;
  private final RecordingOptions recordingOptions;
  private final RecordingConfiguration recordingConfiguration;

  private volatile long id = -1;
  private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

  /**
   * Create a {@code Recording}. Recordings are created from {@link
   * FlightRecorderConnection#newRecording(RecordingOptions, RecordingConfiguration)}
   *
   * @param connection A connection to the FlightRecorderMXBean on an MBean server
   * @param recordingOptions The options to be used for the recording
   * @param recordingConfiguration The settings for events to be collected by the recording
   */
  /* package scope */ Recording(
      FlightRecorderConnection connection,
      RecordingOptions recordingOptions,
      RecordingConfiguration recordingConfiguration) {
    this.connection = connection;
    this.recordingOptions = recordingOptions;
    this.recordingConfiguration = recordingConfiguration;
  }

  /**
   * Get the recording id. The recording does not have an id until the recording is started.
   *
   * @return The recording id, or {@code -1} if the recording was never started.
   */
  public long getId() {
    return id;
  }

  /**
   * Start a recording. A recording may not be started after it is closed.
   *
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws IllegalStateException This {@code Recording} is closed.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   * @return The recording id.
   */
  public long start() throws IOException, JfrConnectionException {
    // state transitions: NEW -> RECORDING or STOPPED -> RECORDING, otherwise remain in state
    State oldState =
        state.getAndUpdate(s -> s == State.NEW || s == State.STOPPED ? State.RECORDING : s);

    if (oldState == State.NEW || oldState == State.STOPPED) {
      id = connection.startRecording(recordingOptions, recordingConfiguration);
    } else if (oldState == State.CLOSED) {
      throw new IllegalStateException(
          createIllegalStateExceptionMessage(oldState, State.NEW, State.RECORDING, State.STOPPED));
    }
    return id;
  }

  /**
   * Stop a recording.
   *
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws IllegalStateException If the {@code Recording} is closed.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   */
  public void stop() throws IOException, JfrConnectionException {
    // state transitions:  RECORDING -> STOPPED, otherwise remain in state
    State oldState = state.getAndUpdate(s -> s == State.RECORDING ? State.STOPPED : s);
    if (oldState == State.RECORDING) {
      connection.stopRecording(id);
    } else if (oldState == State.CLOSED) {
      throw new IllegalStateException(
          createIllegalStateExceptionMessage(oldState, State.NEW, State.RECORDING, State.STOPPED));
    }
  }

  /**
   * Writes recording data to the specified file. The recording must be started, but not necessarily
   * stopped. The {@code outputFile} argument is relevant to the machine where the JVM is running.
   *
   * @param outputFile the system-dependent file name where data is written, not {@code null}
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws IllegalStateException If the {@code Recording} has not been started, or has been
   *     closed.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   * @throws NullPointerException If the {@code outputFile} argument is null.
   */
  public void dump(String outputFile) throws IOException, JfrConnectionException {
    Objects.requireNonNull(outputFile, "outputFile may not be null");
    State currentState = state.get();
    if (currentState == State.RECORDING || currentState == State.STOPPED) {
      connection.dumpRecording(id, outputFile);
    } else {
      throw new IllegalStateException(
          createIllegalStateExceptionMessage(currentState, State.RECORDING, State.STOPPED));
    }
  }

  /**
   * Creates a copy of an existing recording, useful for extracting parts of a recording. The cloned
   * recording contains the same recording data as the original, but it has a new ID. If the
   * original recording is running, then the clone is also running.
   *
   * @param stop Whether to stop the cloned recording.
   * @return The cloned recording.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws IllegalStateException If the {@code Recording} has not been started, or has been
   *     closed.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   */
  public Recording clone(boolean stop) throws IOException, JfrConnectionException {
    State currentState = state.get();
    if (currentState == State.RECORDING || currentState == State.STOPPED) {
      long newId = connection.cloneRecording(id, stop);
      Recording recordingClone =
          new Recording(this.connection, this.recordingOptions, this.recordingConfiguration);
      recordingClone.id = newId;
      recordingClone.state.set(stop ? State.STOPPED : currentState);
      return recordingClone;
    } else {
      throw new IllegalStateException(
          createIllegalStateExceptionMessage(currentState, State.RECORDING, State.STOPPED));
    }
  }

  /**
   * Create a data stream for the specified interval using the default {@code blockSize}. The stream
   * may contain some data outside the given range.
   *
   * @param startTime The start time for the stream, or {@code null} to get data from the start time
   *     of the recording.
   * @param endTime The end time for the stream, or {@code null} to get data until the end of the
   *     recording.
   * @return An {@code InputStream}, or {@code null} if no data is available in the interval.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws IllegalStateException If the {@code Recording} has not been stopped.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   * @see JfrStream#getDefaultBlockSize()
   */
  public InputStream getStream(Instant startTime, Instant endTime)
      throws IOException, JfrConnectionException {
    return getStream(startTime, endTime, JfrStream.getDefaultBlockSize());
  }

  /**
   * Create a data stream for the specified interval using the given {@code blockSize}. The stream
   * may contain some data outside the given range. The {@code blockSize} is used to configure the
   * maximum number of bytes to read from underlying stream at a time. Setting blockSize to a very
   * high value may result in an exception if the Java Virtual Machine (JVM) deems the value too
   * large to handle. Refer to the javadoc for {@code
   * jdk.management.jfr.FlightRecorderMXBean#openStream}.
   *
   * @param startTime The start time for the stream, or {@code null} to get data from the start time
   *     of the recording.
   * @param endTime The end time for the stream, or {@code null} to get data until the end of the
   *     recording.
   * @param blockSize The maximum number of bytes to read at a time.
   * @return An {@code InputStream}, or {@code null} if no data is available in the interval.
   * @throws IOException A communication problem occurred when talking to the MBean server.
   * @throws IllegalStateException If the {@code Recording} has not been stopped.
   * @throws JfrConnectionException Wraps a {@code javax.management.JMException}.
   */
  public InputStream getStream(Instant startTime, Instant endTime, long blockSize)
      throws IOException, JfrConnectionException {
    // state transitions: remain in state
    State currentState = state.get();
    if (currentState == State.STOPPED) {
      return connection.getStream(id, startTime, endTime, blockSize);
    } else {
      throw new IllegalStateException(
          createIllegalStateExceptionMessage(currentState, State.STOPPED));
    }
  }

  /**
   * Get the current state of this {@code Recording}.
   *
   * @return The current state of this {@code Recording}.
   */
  @Nullable
  public State getState() {
    return state.get();
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws IOException, JfrConnectionException {
    // state transitions:  any -> CLOSED
    State oldState = state.getAndSet(State.CLOSED);
    if (oldState == State.RECORDING) {
      try {
        connection.stopRecording(id);
      } catch (IOException | JfrConnectionException ignored) {
        // Stopping the recording is best-effort
      } finally {
        connection.closeRecording(id);
      }
    }
  }
}
