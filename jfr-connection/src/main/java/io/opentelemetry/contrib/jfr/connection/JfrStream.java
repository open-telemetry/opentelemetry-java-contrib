/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.io.IOException;
import java.io.InputStream;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/** A JFR data stream backed by {@code jdk.management.jfr.FlightRecorderMXBean#readStream(long)}. */
@SuppressWarnings("InputStreamSlowMultibyteRead")
class JfrStream extends InputStream {

  /* A default value for blockSize used by FlightRecorderMXBean#readStream(long) */
  private static final long DEFAULT_BLOCKSIZE = Long.getLong("jfr.stream.blocksize", 50000L);

  /**
   * Get the default value for blockSize used to configure the FlightRecorderMXBean#readStream(long)
   * stream. The default is configurable by setting the {@code jfr.stream.blocksize} system
   * property. The {@code blockSize} is used to configure the maximum number of bytes to read from
   * underlying stream at a time. Setting blockSize to a very high value may result in an exception
   * if the Java Virtual Machine (JVM) deems the value too large to handle. Refer to the javadoc for
   * {@code jdk.management.jfr.FlightRecorderMXBean#openStream}.
   *
   * @return The default blockSize for reading flight recording data
   */
  public static long getDefaultBlockSize() {
    return DEFAULT_BLOCKSIZE;
  }

  // Initialize buffer to empty array to subvert null checks.
  private byte[] buffer = new byte[0];
  private int index = 0;
  private boolean reachedEOF = false;
  // There is a recording id and an id you get from the recording for the stream.
  // streamId is the id for the stream.
  private final long streamid;
  private final MBeanServerConnection connection;
  private final ObjectName flightRecorder;

  /* package scope */ JfrStream(
      MBeanServerConnection connection, ObjectName flightRecorder, long streamid) {
    this.streamid = streamid;
    this.connection = connection;
    this.flightRecorder = flightRecorder;
  }

  @Override
  public int read() throws IOException {

    if (!reachedEOF && index == 0) {
      Object[] params = new Object[] {streamid};
      String[] signature = new String[] {long.class.getName()};
      try {
        buffer = (byte[]) connection.invoke(flightRecorder, "readStream", params, signature);
      } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
        throw new IOException(e.getMessage(), e);
      }
    }

    if (reachedEOF || (reachedEOF = (buffer == null))) {
      return -1;
    }

    int b = buffer[index] & 0xFF;
    index = ++index % buffer.length;
    return b;
  }

  @Override
  public void close() throws IOException {
    Object[] params = new Object[] {streamid};
    String[] signature = new String[] {long.class.getName()};
    try {
      connection.invoke(flightRecorder, "closeStream", params, signature);
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw new IOException(e.getMessage(), e);
    }
  }
}
