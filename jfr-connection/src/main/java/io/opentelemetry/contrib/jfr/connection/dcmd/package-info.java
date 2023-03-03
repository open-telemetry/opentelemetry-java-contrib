/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * This package provides API for controlling Java Flight Recordings (JFR) through the
 * DiagnosticCommand MBean. The code in this package is meant to provide a fallback for starting,
 * stopping, and dumping a Java Flight Recording if {@link
 * io.opentelemetry.contrib.jfr.connection.FlightRecorderConnection#connect(javax.management.MBeanServerConnection)}
 * throws an {@code InstanceNotFoundException} on a Java 8 JVM.
 */
package io.opentelemetry.contrib.jfr.connection.dcmd;
