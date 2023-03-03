/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import javax.annotation.Nullable;

/**
 * An JfrStreamingException is a wrapper around specific {@code javax.management.JMException}
 * instances which might be thrown, either directly or indirectly, by methods of this package.
 * Exceptions of this type are not expected from a well behaved JVM.
 *
 * <p>The cause of an {@code JfrStreamingException} will be one of the following:
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
 *       bug in the com.microsoft.jfr package code.
 *   <dt><em>javax.management.openmbean.OpenDataException</em>
 *   <dd>This exception is thrown when an open type, an open data or an open MBean metadata info
 *       instance could not be constructed because one or more validity constraints were not met.
 *       This cause indicates a bug in the com.microsoft.jfr package code.
 * </dl>
 */
public class JfrStreamingException extends Exception {

  private static final long serialVersionUID = 7394612902107510439L;

  /**
   * Construct a {@code JfrStreamingException} with a message and cause.
   *
   * @param message The exception message.
   * @param cause The cause of the exception.
   */
  public JfrStreamingException(@Nullable String message, Exception cause) {
    super(message, cause);
  }

  /**
   * Construct a {@code JfrStreamingException} with a message only.
   *
   * @param message The exception message.
   */
  public JfrStreamingException(@Nullable String message) {
    super(message);
  }
}
