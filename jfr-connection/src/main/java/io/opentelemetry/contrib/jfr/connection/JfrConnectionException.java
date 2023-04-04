/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An JfrConnectionException is a wrapper around exceptions which might be thrown, either directly
 * or indirectly, by methods of this package.
 *
 * <p>The {@link JfrConnectionException#getCause() cause} of a {@code JfrConnectionException} may
 * be, but is not limited to, one of the following:
 *
 * <dl>
 *   <dt><em>javax.management.InstanceNotFoundException</em>
 *   <dd>The FlightRecorderMXBean is not found on the MBean server. This could happen if the target
 *       JVM does not support Java Flight Recorder, or if experimental features need to be enabled
 *       on the target JVM. An InstanceNotFoundException is not expected after the connection is
 *       made to the FlightRecorderMXBean.
 *   <dt><em>javax.management.MBeanException</em>
 *   <dd>Represents "user defined" exceptions thrown by MBean methods in the agent. It "wraps" the
 *       actual "user defined" exception thrown. This exception will be built by the MBeanServer
 *       when a call to an MBean method results in an unknown exception.
 *   <dt><em>javax.management.ReflectionException</em>
 *   <dd>Represents exceptions thrown in the MBean server when using the java.lang.reflect classes
 *       to invoke methods on MBeans. It "wraps" the actual java.lang.Exception thrown.
 *   <dt><em>javax.management.MalformedObjectNameException</em>
 *   <dd>The format of the string does not correspond to a valid ObjectName. This cause indicates a
 *       bug in the io.opentelemetry.contrib.jfr.connection package code.
 *   <dt><em>javax.management.openmbean.OpenDataException</em>
 *   <dd>This exception is thrown when an open type, an open data or an open MBean metadata info
 *       instance could not be constructed because one or more validity constraints were not met.
 *       This cause indicates a bug in the io.opentelemetry.contrib.jfr.connection package code.
 * </dl>
 */
public class JfrConnectionException extends Exception {

  private static final long serialVersionUID = 7394612902107510439L;

  /**
   * Construct a {@code JfrConnectionException} with a message and cause.
   *
   * @param message The exception message.
   * @param cause The cause of the exception.
   */
  public JfrConnectionException(@Nullable String message, Exception cause) {
    super(message, cause);
  }

  /**
   * Construct a {@code JfrConnectionException} with a message only.
   *
   * @param message The exception message.
   */
  public JfrConnectionException(@Nullable String message) {
    super(message);
  }

  /**
   * Convenience method to create a JfrConnectionException with a canonical message and a cause.
   *
   * @param clazz The class that caught the {@code cause}.
   * @param methodName The name of the method that caught the {@code cause}.
   * @param cause The exception that was caught.
   * @return A JfrConnectionException with a canonical message.
   */
  /* package scope */ static JfrConnectionException canonicalJfrConnectionException(
      @Nonnull Class<?> clazz, @Nonnull String methodName, @Nonnull Exception cause) {
    String canonicalMessage =
        String.format("%1s.%2s caught: %3s", clazz.getSimpleName(), methodName, cause.getMessage());
    return new JfrConnectionException(canonicalMessage, cause);
  }
}
